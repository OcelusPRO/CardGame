package fr.ftnl.cardgame.ws

import fr.ftnl.cardgame.api.dto.AvatarInput
import fr.ftnl.cardgame.api.dto.CreateGameRequest
import fr.ftnl.cardgame.api.dto.GameSettingsInput
import fr.ftnl.cardgame.api.dto.GameTicket
import fr.ftnl.cardgame.api.dto.GameView
import fr.ftnl.cardgame.catalog.CardPack
import fr.ftnl.cardgame.catalog.CatalogSituation
import fr.ftnl.cardgame.domain.card.CardId
import fr.ftnl.cardgame.domain.card.SituationText
import fr.ftnl.cardgame.domain.game.AnswerMode
import fr.ftnl.cardgame.support.awaitFailure
import fr.ftnl.cardgame.support.awaitState
import fr.ftnl.cardgame.support.browser
import fr.ftnl.cardgame.support.emit
import fr.ftnl.cardgame.support.seedTestDeck
import fr.ftnl.cardgame.support.TWITCH_LOGIN_PATH
import fr.ftnl.cardgame.support.startTestServer
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** End to end check of the socket protocol: two browsers really play a round. */
class GameSocketTest {

    private val avatar = AvatarInput("head-1", "#ff8800", "body-1", "#3355ff")

    @Test
    fun `two players play a round from the lobby to the score`() = testApplication {
        val services = startTestServer()
        runBlocking { services.seedTestDeck() }
        val host = browser()
        val guest = browser()
        val code = host.createGame("Alice").code
        guest.join(code, "Bob")

        val hostSocket = host.webSocketSession("/ws/game/$code")
        val guestSocket = guest.webSocketSession("/ws/game/$code")

        hostSocket.emit(ClientMessage.Start)
        val dealt = hostSocket.awaitState { it.phase == "SUBMITTING" }
        val guestDealt = guestSocket.awaitState { it.phase == "SUBMITTING" }
        assertEquals(10, dealt.you.hand.size)

        hostSocket.emit(ClientMessage.PlayCards(listOf(dealt.you.hand.first().id)))
        guestSocket.emit(ClientMessage.PlayCards(listOf(guestDealt.you.hand.first().id)))

        val voting = hostSocket.awaitState { it.phase == "SELECTING" }
        assertEquals(2, voting.round?.answers?.size)
        assertTrue(voting.round?.answers.orEmpty().all { it.authorId == null })

        hostSocket.emit(ClientMessage.Choose(voting.otherAnswerId()))
        val guestVoting = guestSocket.awaitState { it.phase == "SELECTING" }
        guestSocket.emit(ClientMessage.Choose(guestVoting.otherAnswerId()))

        // Two players can only vote for each other, so each answer took every vote its
        // author was not allowed to cast: both are unanimous, both take the bonus.
        val scored = hostSocket.awaitState { it.phase == "ROUND_RESULT" }
        val expected = 1 * scored.settings.pointsPerVote + scored.settings.unanimityBonus
        assertTrue(scored.players.all { it.score == expected }, "scores were ${scored.players.map { it.score }}")
        assertTrue(scored.round?.answers.orEmpty().all { it.authorId != null })

        hostSocket.cancel()
        guestSocket.cancel()
    }

    @Test
    fun `a ping is answered so the client can watch the link`() = testApplication {
        val services = startTestServer()
        runBlocking { services.seedTestDeck() }
        val host = browser()
        val code = host.createGame("Alice").code

        val socket = host.webSocketSession("/ws/game/$code")
        socket.awaitState()
        socket.emit(ClientMessage.Ping)

        socket.emit(ClientMessage.Start)
        assertEquals("NOT_ENOUGH_PLAYERS", socket.awaitFailure())
        socket.cancel()
    }

    @Test
    fun `the host can retune the settings from the lobby`() = testApplication {
        val services = startTestServer()
        runBlocking { services.seedTestDeck() }
        val host = browser()
        val code = host.createGame("Alice").code

        val socket = host.webSocketSession("/ws/game/$code")
        socket.awaitState()
        socket.emit(ClientMessage.UpdateSettings(GameSettingsInput(rounds = 7, answerMode = "FREE_TEXT")))

        val updated = socket.awaitState { it.settings.rounds == 7 }
        assertEquals("FREE_TEXT", updated.settings.answerMode)
        socket.cancel()
    }

    @Test
    fun `switching the answer mode drops a pack reserved to the other one`() = testApplication {
        val services = startTestServer()
        runBlocking {
            services.seedTestDeck() // pack "test", both modes, 5 situations
            services.packRepository.save(
                CardPack("cards-only", "Cartes seules", answerModes = setOf(AnswerMode.CARDS)),
            )
            repeat(3) { index ->
                services.situationRepository.save(
                    CatalogSituation(CardId("co-s${index + 1}"), "cards-only", SituationText("____ ?")),
                )
            }
        }
        val host = browser()
        val code = host.createGame("Alice").code

        val socket = host.webSocketSession("/ws/game/$code")
        assertEquals(8, socket.awaitState().deck.situationsLeft)

        socket.emit(ClientMessage.UpdateSettings(GameSettingsInput(answerMode = "FREE_TEXT")))
        val pruned = socket.awaitState { it.settings.answerMode == "FREE_TEXT" && it.deck.situationsLeft == 5 }

        assertEquals(5, pruned.deck.situationsLeft)
        socket.cancel()
    }

    @Test
    fun `typing a secret code pulls the hidden pack into the live deck`() = testApplication {
        val services = startTestServer()
        runBlocking {
            services.seedTestDeck() // pack "test", both modes, 5 situations
            services.packRepository.save(
                CardPack("hidden", "Caché", secretCode = "sesame"),
            )
            repeat(3) { index ->
                services.situationRepository.save(
                    CatalogSituation(CardId("h-s${index + 1}"), "hidden", SituationText("____ ?")),
                )
            }
        }
        val host = browser()
        val code = host.createGame("Alice").code

        val socket = host.webSocketSession("/ws/game/$code")
        assertEquals(5, socket.awaitState().deck.situationsLeft)

        socket.emit(
            ClientMessage.UpdateDeck(
                fr.ftnl.cardgame.api.dto.DeckInput(
                    packIds = setOf("test"),
                    customSituations = listOf("sesame"),
                ),
            ),
        )

        // The 3 hidden situations join; the "sesame" line itself is not turned into a card.
        val unlocked = socket.awaitState { it.deck.situationsLeft == 8 }
        assertEquals(8, unlocked.deck.situationsLeft)
        socket.cancel()
    }

    @Test
    fun `a streaming host puts their chat on the table`() = testApplication {
        val services = startTestServer()
        runBlocking { services.seedTestDeck() }
        val host = browser()
        host.get("$TWITCH_LOGIN_PATH?login=kameto")
        val code = host.createGame("Alice").code

        val socket = host.webSocketSession("/ws/game/$code")
        val seated = socket.awaitState()
        assertEquals("kameto", seated.players.single().twitchLogin)
        // Nothing is read until the host asks for it.
        assertTrue(seated.chatChannels.isEmpty())

        socket.emit(ClientMessage.UpdateSettings(GameSettingsInput(selectionMode = "CHAT")))
        val watching = socket.awaitState { it.settings.selectionMode == "CHAT" }

        assertEquals(listOf("kameto"), watching.chatChannels)
        socket.cancel()
    }

    @Test
    fun `a guest cannot start the game`() = testApplication {
        val services = startTestServer()
        runBlocking { services.seedTestDeck() }
        val host = browser()
        val guest = browser()
        val code = host.createGame("Alice").code
        guest.join(code, "Bob")

        val socket = guest.webSocketSession("/ws/game/$code")
        socket.awaitState()
        socket.emit(ClientMessage.Start)

        assertEquals("NOT_THE_HOST", socket.awaitFailure())
        socket.cancel()
    }

    private fun GameView.otherAnswerId(): Int =
        round?.answers?.first { !it.isMine }?.id ?: error("no answer to vote for")

    private suspend fun HttpClient.createGame(nickname: String): GameTicket = post("/api/games") {
        contentType(ContentType.Application.Json)
        setBody(CreateGameRequest(nickname, avatar, GameSettingsInput(minPlayers = 2)))
    }.body()

    private suspend fun HttpClient.join(code: String, nickname: String) {
        post("/api/games/$code/players") {
            contentType(ContentType.Application.Json)
            setBody(fr.ftnl.cardgame.api.dto.JoinGameRequest(nickname, avatar))
        }
    }
}
