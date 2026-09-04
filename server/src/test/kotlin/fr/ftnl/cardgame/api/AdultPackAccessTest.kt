package fr.ftnl.cardgame.api

import fr.ftnl.cardgame.api.dto.AdultAccessInput
import fr.ftnl.cardgame.api.dto.AdultAccessView
import fr.ftnl.cardgame.api.dto.AvatarInput
import fr.ftnl.cardgame.api.dto.CardPackView
import fr.ftnl.cardgame.api.dto.CreateGameRequest
import fr.ftnl.cardgame.api.dto.GameSettingsInput
import fr.ftnl.cardgame.api.dto.GameTicket
import fr.ftnl.cardgame.api.dto.JoinGameRequest
import fr.ftnl.cardgame.api.dto.PackAdminView
import fr.ftnl.cardgame.api.dto.PackInput
import fr.ftnl.cardgame.support.DISCORD_LOGIN_PATH
import fr.ftnl.cardgame.support.TWITCH_LOGIN_PATH
import fr.ftnl.cardgame.support.TestConfig
import fr.ftnl.cardgame.support.adminBrowser
import fr.ftnl.cardgame.support.browser
import fr.ftnl.cardgame.support.startTestServer
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Packs flagged "interdit aux mineurs" are only listed for a host whose Discord account
 * is an administrator or sits on the allowlist managed from the administration.
 */
class AdultPackAccessTest {

    private suspend fun createAdultPack(admin: io.ktor.client.HttpClient): String =
        admin.post("/api/admin/packs") {
            contentType(ContentType.Application.Json)
            setBody(PackInput(name = "18+", description = "", enabled = true, adultOnly = true))
        }.body<PackAdminView>().id

    @Test
    fun `an adult pack is hidden from an anonymous host`() = testApplication {
        startTestServer()
        val admin = adminBrowser()
        val packId = createAdultPack(admin)

        val packs = browser().get("/api/packs").body<List<CardPackView>>()

        assertFalse(packs.any { it.id == packId }, "the anonymous list must not leak the 18+ pack")
    }

    @Test
    fun `an admin sees the adult pack, flagged`() = testApplication {
        startTestServer()
        val admin = adminBrowser()
        val packId = createAdultPack(admin)

        val packs = admin.get("/api/packs").body<List<CardPackView>>()

        val adult = packs.single { it.id == packId }
        assertTrue(adult.adultOnly)
    }

    @Test
    fun `a listed Discord account gains access, and loses it once removed`() = testApplication {
        startTestServer()
        val admin = adminBrowser()
        val packId = createAdultPack(admin)
        val user = browser()
        user.get("$DISCORD_LOGIN_PATH?id=100000000000000042")

        assertFalse(user.get("/api/packs").body<List<CardPackView>>().any { it.id == packId })

        val added = admin.post("/api/admin/adult-access") {
            contentType(ContentType.Application.Json)
            setBody(AdultAccessInput(accountId = "100000000000000042", label = "Alex"))
        }.body<AdultAccessView>()
        assertEquals("Alex", added.label)

        assertTrue(
            user.get("/api/packs").body<List<CardPackView>>().any { it.id == packId },
            "once on the allowlist the host should see the 18+ pack",
        )

        assertEquals(
            HttpStatusCode.NoContent,
            admin.delete("/api/admin/adult-access/DISCORD/100000000000000042").status,
        )
        assertFalse(user.get("/api/packs").body<List<CardPackView>>().any { it.id == packId })
    }

    @Test
    fun `a listed Twitch account gains access just like a Discord one`() = testApplication {
        startTestServer()
        val admin = adminBrowser()
        val packId = createAdultPack(admin)
        val streamer = browser()
        streamer.get("$TWITCH_LOGIN_PATH?login=kameto&id=44444")

        assertFalse(streamer.get("/api/packs").body<List<CardPackView>>().any { it.id == packId })

        admin.post("/api/admin/adult-access") {
            contentType(ContentType.Application.Json)
            setBody(AdultAccessInput(provider = "TWITCH", accountId = "44444", label = "Kameto"))
        }

        assertTrue(
            streamer.get("/api/packs").body<List<CardPackView>>().any { it.id == packId },
            "once on the allowlist the Twitch host should see the 18+ pack",
        )

        assertEquals(
            HttpStatusCode.NoContent,
            admin.delete("/api/admin/adult-access/TWITCH/44444").status,
        )
        assertFalse(streamer.get("/api/packs").body<List<CardPackView>>().any { it.id == packId })
    }

    @Test
    fun `a Twitch account past three years is trusted on its age alone`() = testApplication {
        startTestServer(TestConfig.trustingAccountAge())
        val admin = adminBrowser()
        val packId = createAdultPack(admin)
        val streamer = browser()
        val fourYearsAgo = System.currentTimeMillis() - 4L * 365 * 24 * 60 * 60 * 1000

        streamer.get("$TWITCH_LOGIN_PATH?login=ancien&id=55555&createdAt=$fourYearsAgo")

        assertTrue(
            streamer.get("/api/packs").body<List<CardPackView>>().any { it.id == packId },
            "an account opened four years ago is old enough for the 18+ packs",
        )
    }

    @Test
    fun `a guest lobby list follows the host, not the guest's own clearance`() = testApplication {
        startTestServer()
        val admin = adminBrowser()
        val adultPackId = createAdultPack(admin)
        val plainPackId = admin.post("/api/admin/packs") {
            contentType(ContentType.Application.Json)
            setBody(PackInput(name = "Tout public", description = "", enabled = true))
        }.body<PackAdminView>().id

        // A guest who is personally cleared for the 18+ pack.
        val guest = browser()
        guest.get("$DISCORD_LOGIN_PATH?id=100000000000000042")
        admin.post("/api/admin/adult-access") {
            contentType(ContentType.Application.Json)
            setBody(AdultAccessInput(accountId = "100000000000000042", label = "Alex"))
        }
        assertTrue(guest.get("/api/packs").body<List<CardPackView>>().any { it.id == adultPackId })

        // An anonymous host opens a table: their paquet cannot contain the 18+ pack.
        val host = browser()
        val code = host.post("/api/games") {
            contentType(ContentType.Application.Json)
            setBody(
                CreateGameRequest(
                    "Alice",
                    AvatarInput("head-1", "#fff", "body-1", "#000"),
                    GameSettingsInput(minPlayers = 2),
                ),
            )
        }.body<GameTicket>().code
        guest.post("/api/games/$code/players") {
            contentType(ContentType.Application.Json)
            setBody(JoinGameRequest("Bob", AvatarInput("head-1", "#fff", "body-1", "#000")))
        }

        val seenByGuest = guest.get("/api/packs?code=$code").body<List<CardPackView>>()
        assertTrue(seenByGuest.any { it.id == plainPackId }, "the guest still sees the host's public packs")
        assertFalse(
            seenByGuest.any { it.id == adultPackId },
            "a guest must not see a pack the host has no access to, even one they are cleared for",
        )
    }

    @Test
    fun `the allowlist is reserved to administrators`() = testApplication {
        startTestServer()

        assertEquals(HttpStatusCode.Forbidden, browser().get("/api/admin/adult-access").status)
    }

    @Test
    fun `a non numeric account id is refused`() = testApplication {
        startTestServer()

        val response = adminBrowser().post("/api/admin/adult-access") {
            contentType(ContentType.Application.Json)
            setBody(AdultAccessInput(accountId = "not-an-id"))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
