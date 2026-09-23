package fr.ftnl.cardgame.ws

import fr.ftnl.cardgame.api.dto.AvatarInput
import fr.ftnl.cardgame.api.dto.CreateGameRequest
import fr.ftnl.cardgame.api.dto.GamePreview
import fr.ftnl.cardgame.api.dto.GameSettingsInput
import fr.ftnl.cardgame.api.dto.GameTicket
import fr.ftnl.cardgame.api.dto.JoinGameRequest
import fr.ftnl.cardgame.support.awaitFailure
import fr.ftnl.cardgame.support.awaitState
import fr.ftnl.cardgame.support.browser
import fr.ftnl.cardgame.support.emit
import fr.ftnl.cardgame.support.seedTestDeck
import fr.ftnl.cardgame.support.startTestServer
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A table played on one phone passed round the room, and a host trading their seat for the
 * stream page. Both come down to the same promise: a socket only ever looks through the
 * seat it was handed, and the screen facing the room shows nobody's cards.
 */
class SharedDeviceSocketTest {

    private val avatar = AvatarInput("head-1", "#ff8800", "body-1", "#3355ff")

    @Test
    fun `the phone goes round, each player seeing only their own hand`() = testApplication {
        val services = startTestServer()
        runBlocking { services.seedTestDeck() }
        val device = browser()
        val code = device.createGame("Alice", sharedDevice = true).code
        val socket = device.webSocketSession("/ws/game/$code")

        socket.emit(ClientMessage.AddSeat("Bob", avatar))
        val lobby = socket.awaitState { it.players.size == 2 }
        assertTrue(lobby.sharedDevice)
        val (alice, bob) = lobby.players.map { it.id }

        socket.emit(ClientMessage.Start)
        // Facing the room, the phone shows no hand and waits on both players in seat order.
        val table = socket.awaitState { it.phase == "SUBMITTING" }
        assertTrue(table.you.hand.isEmpty(), "the table screen showed a hand")
        assertEquals(listOf(alice, bob), table.awaiting)
        assertNull(table.deadlineMillis, "a shared device should not answer against the clock")

        socket.emit(ClientMessage.Seat(bob))
        val bobsTurn = socket.awaitState { it.you.id == bob }
        assertTrue(bobsTurn.you.mustAnswer)
        socket.emit(ClientMessage.PlayCards(listOf(bobsTurn.you.hand.first().id)))
        assertEquals(listOf(alice), socket.awaitState { bob !in it.awaiting }.awaiting)

        socket.emit(ClientMessage.Seat(alice))
        val alicesTurn = socket.awaitState { it.you.id == alice && it.you.mustAnswer }
        socket.emit(ClientMessage.PlayCards(listOf(alicesTurn.you.hand.first().id)))

        // Judging: back on the table screen, no answer is anybody's.
        socket.emit(ClientMessage.Seat(null))
        val voting = socket.awaitState { it.phase == "SELECTING" && it.you.id == alice && !it.you.mustVote }
        assertTrue(voting.round?.answers.orEmpty().none { it.isMine })

        socket.cancel()
    }

    @Test
    fun `nobody else joins a shared device, and an online table lends no seat`() = testApplication {
        val services = startTestServer()
        runBlocking { services.seedTestDeck() }
        val device = browser()
        val shared = device.createGame("Alice", sharedDevice = true).code

        val stranger = browser()
        assertFalse(stranger.get("/api/games/$shared").body<GamePreview>().canJoin)
        assertEquals(HttpStatusCode.Conflict, stranger.join(shared, "Bob"))

        // On an online table, asking to look through somebody else's seat is refused.
        val host = browser()
        val guest = browser()
        val online = host.createGame("Carl").code
        guest.join(online, "Dana")
        val socket = host.webSocketSession("/ws/game/$online")
        val lobby = socket.awaitState()
        socket.emit(ClientMessage.Seat(lobby.players.first { it.nickname == "Dana" }.id))
        assertEquals("NOT_THE_HOST", socket.awaitFailure())

        socket.cancel()
    }

    @Test
    fun `a host steps aside for the phone they will play from`() = testApplication {
        val services = startTestServer()
        runBlocking { services.seedTestDeck() }
        val computer = browser()
        val phone = browser()
        val code = computer.createGame("Stream").code
        phone.join(code, "Phone")
        val socket = computer.webSocketSession("/ws/game/$code")
        val phoneId = socket.awaitState { it.players.size == 2 }.players.first { it.nickname == "Phone" }.id

        socket.emit(ClientMessage.StepAside(heir = phoneId))
        socket.awaitState { it.players.size == 1 }
        socket.cancel()

        val phoneSocket = phone.webSocketSession("/ws/game/$code")
        val after = phoneSocket.awaitState { it.players.size == 1 }
        assertEquals(phoneId, after.hostId)
        assertTrue(after.you.isHost)

        phoneSocket.cancel()
    }

    private suspend fun HttpClient.createGame(nickname: String, sharedDevice: Boolean = false): GameTicket =
        post("/api/games") {
            contentType(ContentType.Application.Json)
            setBody(CreateGameRequest(nickname, avatar, GameSettingsInput(minPlayers = 2), sharedDevice))
        }.body()

    private suspend fun HttpClient.join(code: String, nickname: String): HttpStatusCode =
        post("/api/games/$code/players") {
            contentType(ContentType.Application.Json)
            setBody(JoinGameRequest(nickname, avatar))
        }.status
}
