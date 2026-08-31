package fr.ftnl.cardgame.api

import fr.ftnl.cardgame.api.dto.AvatarInput
import fr.ftnl.cardgame.api.dto.CreateGameRequest
import fr.ftnl.cardgame.api.dto.ErrorResponse
import fr.ftnl.cardgame.api.dto.GamePreview
import fr.ftnl.cardgame.api.dto.GameSettingsInput
import fr.ftnl.cardgame.api.dto.GameTicket
import fr.ftnl.cardgame.api.dto.JoinGameRequest
import fr.ftnl.cardgame.support.browser
import fr.ftnl.cardgame.support.startTestServer
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameRoutesTest {

    private val avatar = AvatarInput("head-1", "#ff8800", "body-1", "#3355ff")

    @Test
    fun `creating a game hands back a code and the address that is the invitation`() = testApplication {
        startTestServer()
        val host = browser()

        val ticket = host.createGame("Alice").body<GameTicket>()

        assertEquals(5, ticket.code.length)
        assertTrue(ticket.isHost)
        assertTrue(ticket.joinUrl.endsWith("/game/${ticket.code}"))
    }

    @Test
    fun `a second player joins with the code`() = testApplication {
        startTestServer()
        val ticket = browser().createGame("Alice").body<GameTicket>()

        val guest = browser().join(ticket.code, "Bob").body<GameTicket>()

        assertEquals(ticket.code, guest.code)
        assertTrue(!guest.isHost)
    }

    @Test
    fun `the preview tells the join screen what to display`() = testApplication {
        startTestServer()
        val ticket = browser().createGame("Alice").body<GameTicket>()

        val preview = browser().get("/api/games/${ticket.code}").body<GamePreview>()

        assertEquals("LOBBY", preview.phase)
        assertEquals("Alice", preview.hostNickname)
        assertTrue(preview.canJoin)
    }

    @Test
    fun `the preview tells a newcomer they are not at the table yet`() = testApplication {
        startTestServer()
        val ticket = browser().createGame("Alice").body<GameTicket>()

        val newcomer = browser().get("/api/games/${ticket.code}").body<GamePreview>()

        assertTrue(!newcomer.youArePlaying)
    }

    @Test
    fun `the preview recognises a player already seated`() = testApplication {
        startTestServer()
        val host = browser()
        val ticket = host.createGame("Alice").body<GameTicket>()

        val preview = host.get("/api/games/${ticket.code}").body<GamePreview>()

        assertTrue(preview.youArePlaying)
    }

    @Test
    fun `an unknown code answers 404`() = testApplication {
        startTestServer()

        val response = browser().get("/api/games/ZZZZZ")

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals("GAME_NOT_FOUND", response.body<ErrorResponse>().code)
    }

    @Test
    fun `a malformed code answers 404 rather than a server error`() = testApplication {
        startTestServer()

        assertEquals(HttpStatusCode.NotFound, browser().get("/api/games/oops").status)
    }

    @Test
    fun `a nickname already at the table is refused`() = testApplication {
        startTestServer()
        val ticket = browser().createGame("Alice").body<GameTicket>()

        val response = browser().join(ticket.code, "Alice")

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertEquals("NICKNAME_TAKEN", response.body<ErrorResponse>().code)
    }

    @Test
    fun `an empty nickname is refused with a validation error`() = testApplication {
        startTestServer()

        val response = browser().createGame(" ")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("VALIDATION_ERROR", response.body<ErrorResponse>().code)
    }

    @Test
    fun `an impossible setting is refused before the game exists`() = testApplication {
        startTestServer()

        val response = browser().post("/api/games") {
            contentType(ContentType.Application.Json)
            setBody(CreateGameRequest("Alice", avatar, GameSettingsInput(handSize = 99)))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    private suspend fun HttpClient.createGame(nickname: String): HttpResponse = post("/api/games") {
        contentType(ContentType.Application.Json)
        setBody(CreateGameRequest(nickname, avatar, GameSettingsInput(minPlayers = 2)))
    }

    private suspend fun HttpClient.join(code: String, nickname: String): HttpResponse =
        post("/api/games/$code/players") {
            contentType(ContentType.Application.Json)
            setBody(JoinGameRequest(nickname, avatar))
        }
}
