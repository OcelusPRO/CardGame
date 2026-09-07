package fr.ftnl.cardgame.ws

import fr.ftnl.cardgame.api.dto.AvatarInput
import fr.ftnl.cardgame.api.dto.CreateGameRequest
import fr.ftnl.cardgame.api.dto.GameSettingsInput
import fr.ftnl.cardgame.api.dto.GameTicket
import fr.ftnl.cardgame.api.dto.JoinGameRequest
import fr.ftnl.cardgame.support.awaitPong
import fr.ftnl.cardgame.support.awaitState
import fr.ftnl.cardgame.support.browser
import fr.ftnl.cardgame.support.emit
import fr.ftnl.cardgame.support.seedTestDeck
import fr.ftnl.cardgame.support.startTestServer
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.ktor.websocket.CloseReason
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The stream feed of a table. What is being checked is one promise: whoever opens
 * `/ws/spectate/CODE` sees the game and never sees a hand — not their own, because they
 * have none, and not anybody else's either.
 */
class SpectatorSocketTest {

    private val avatar = AvatarInput("head-1", "#ff8800", "body-1", "#3355ff")

    @Test
    fun `anybody with the code watches, without a session and without a hand`() = testApplication {
        val services = startTestServer()
        runBlocking { services.seedTestDeck() }
        val host = browser()
        val guest = browser()
        val code = host.createGame("Alice").code
        guest.join(code, "Bob")

        // A brand new client: no cookie has ever been set on it, which is exactly what an
        // OBS browser source is.
        val stream = browser().webSocketSession("/ws/spectate/$code")
        val lobby = stream.awaitState()

        assertTrue(lobby.spectator, "the snapshot should say it was projected for the stream")
        assertEquals("", lobby.you.id)
        assertTrue(lobby.you.hand.isEmpty())
        assertEquals(2, lobby.players.size)

        val hostSocket = host.webSocketSession("/ws/game/$code")
        val guestSocket = guest.webSocketSession("/ws/game/$code")
        hostSocket.emit(ClientMessage.Start)

        // The players are dealt ten cards each; the stream page is dealt none.
        val dealt = hostSocket.awaitState { it.phase == "SUBMITTING" }
        val watched = stream.awaitState { it.phase == "SUBMITTING" }
        assertEquals(10, dealt.you.hand.size)
        assertTrue(watched.you.hand.isEmpty(), "a spectator was dealt a hand")
        assertTrue(watched.round?.answers.orEmpty().isEmpty())

        hostSocket.emit(ClientMessage.PlayCards(listOf(dealt.you.hand.first().id)))
        val guestDealt = guestSocket.awaitState { it.phase == "SUBMITTING" }
        guestSocket.emit(ClientMessage.PlayCards(listOf(guestDealt.you.hand.first().id)))

        // Judging: the answers are on the table for everyone, still tied to nobody — and
        // none of them is marked as the watcher's own, because the watcher has none.
        val voting = stream.awaitState { it.phase == "SELECTING" }
        assertEquals(2, voting.round?.answers?.size)
        assertTrue(voting.round?.answers.orEmpty().all { it.authorId == null })
        assertTrue(voting.round?.answers.orEmpty().none { it.isMine })
        assertNull(voting.round?.myVote)

        hostSocket.cancel()
        guestSocket.cancel()
        stream.cancel()
    }

    @Test
    fun `a command sent on the stream feed changes nothing`() = testApplication {
        val services = startTestServer()
        runBlocking { services.seedTestDeck() }
        val host = browser()
        val guest = browser()
        val code = host.createGame("Alice").code
        guest.join(code, "Bob")

        val stream = browser().webSocketSession("/ws/spectate/$code")
        stream.awaitState()
        stream.emit(ClientMessage.Start)
        // The pong is the receipt: this socket has now handled everything sent before it,
        // so the table below is what the Start left behind, not what it has not reached yet.
        stream.emit(ClientMessage.Ping)
        stream.awaitPong()

        // Nothing came of it — two players and a start command, and still the lobby.
        val hostSocket = host.webSocketSession("/ws/game/$code")
        assertEquals("LOBBY", hostSocket.awaitState().phase)

        hostSocket.cancel()
        stream.cancel()
    }

    @Test
    fun `a ping is answered, so a browser source notices a dead link`() = testApplication {
        val services = startTestServer()
        runBlocking { services.seedTestDeck() }
        val code = browser().createGame("Alice").code

        val stream = browser().webSocketSession("/ws/spectate/$code")
        stream.awaitState()
        stream.emit(ClientMessage.Ping)
        stream.awaitPong()

        stream.cancel()
    }

    @Test
    fun `an unknown table is refused for good rather than left hanging`() = testApplication {
        startTestServer()

        val stream = browser().webSocketSession("/ws/spectate/ZZZZZ")

        assertEquals(CloseReason.Codes.VIOLATED_POLICY.code, stream.closeReason.await()?.code)
    }

    private suspend fun HttpClient.createGame(nickname: String): GameTicket = post("/api/games") {
        contentType(ContentType.Application.Json)
        setBody(CreateGameRequest(nickname, avatar, GameSettingsInput(minPlayers = 2)))
    }.body()

    private suspend fun HttpClient.join(code: String, nickname: String) {
        post("/api/games/$code/players") {
            contentType(ContentType.Application.Json)
            setBody(JoinGameRequest(nickname, avatar))
        }
    }
}
