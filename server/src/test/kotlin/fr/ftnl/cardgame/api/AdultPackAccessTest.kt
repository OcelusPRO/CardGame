package fr.ftnl.cardgame.api

import fr.ftnl.cardgame.api.dto.AdultAccessInput
import fr.ftnl.cardgame.api.dto.AdultAccessView
import fr.ftnl.cardgame.api.dto.CardPackView
import fr.ftnl.cardgame.api.dto.PackAdminView
import fr.ftnl.cardgame.api.dto.PackInput
import fr.ftnl.cardgame.support.DISCORD_LOGIN_PATH
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
            setBody(AdultAccessInput(discordId = "100000000000000042", label = "Alex"))
        }.body<AdultAccessView>()
        assertEquals("Alex", added.label)

        assertTrue(
            user.get("/api/packs").body<List<CardPackView>>().any { it.id == packId },
            "once on the allowlist the host should see the 18+ pack",
        )

        assertEquals(
            HttpStatusCode.NoContent,
            admin.delete("/api/admin/adult-access/100000000000000042").status,
        )
        assertFalse(user.get("/api/packs").body<List<CardPackView>>().any { it.id == packId })
    }

    @Test
    fun `the allowlist is reserved to administrators`() = testApplication {
        startTestServer()

        assertEquals(HttpStatusCode.Forbidden, browser().get("/api/admin/adult-access").status)
    }

    @Test
    fun `a non numeric Discord id is refused`() = testApplication {
        startTestServer()

        val response = adminBrowser().post("/api/admin/adult-access") {
            contentType(ContentType.Application.Json)
            setBody(AdultAccessInput(discordId = "not-an-id"))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
