package fr.ftnl.cardgame.api

import fr.ftnl.cardgame.api.dto.CardAdminView
import fr.ftnl.cardgame.api.dto.CardInput
import fr.ftnl.cardgame.api.dto.CardPackView
import fr.ftnl.cardgame.api.dto.ErrorResponse
import fr.ftnl.cardgame.api.dto.PackAdminView
import fr.ftnl.cardgame.seed.DevDeckSeeder
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
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The bundled demo deck is seeded through the very same repositories an administrator
 * writes to, so it has to behave like any other pack: listed, editable, and deletable
 * only once emptied. These checks pin that down.
 */
class AdminCatalogTest {

    private fun seedDemoDeck(services: fr.ftnl.cardgame.ApplicationServices) = runBlocking {
        DevDeckSeeder(
            packs = services.packRepository,
            situations = services.situationRepository,
            punchlines = services.punchlineRepository,
            clock = services.clock,
        ).seed()
    }

    @Test
    fun `the demo deck is listed in the administration like any other pack`() = testApplication {
        val services = startTestServer()
        assertTrue(seedDemoDeck(services))

        val packs = adminBrowser().get("/api/admin/packs").body<List<PackAdminView>>()

        val demo = packs.single { it.id == "demo-fr" }
        assertEquals("Pack démo (FR)", demo.name)
        assertTrue(demo.enabled)
        assertTrue(demo.situationCount > 20, "the admin should see how big the pack is")
        assertTrue(demo.punchlineCount > 60)
    }

    @Test
    fun `its cards are listed in the administration tables`() = testApplication {
        val services = startTestServer()
        seedDemoDeck(services)
        val admin = adminBrowser()

        val situations = admin.get("/api/admin/situations").body<List<CardAdminView>>()
        val punchlines = admin.get("/api/admin/punchlines").body<List<CardAdminView>>()

        assertTrue(situations.all { it.packId == "demo-fr" })
        assertTrue(situations.size > 20, "expected the demo situations, got ${situations.size}")
        assertTrue(punchlines.size > 60, "expected the demo punchlines, got ${punchlines.size}")
        assertTrue(situations.all { (it.blankCount ?: 0) >= 1 })
    }

    @Test
    fun `the same deck is offered to hosts in the lobby`() = testApplication {
        val services = startTestServer()
        seedDemoDeck(services)

        val packs = browser().get("/api/packs").body<List<CardPackView>>()

        val demo = packs.single { it.id == "demo-fr" }
        assertTrue(demo.situationCount > 20)
        assertTrue(demo.punchlineCount > 60)
    }

    @Test
    fun `a card can be added to the demo deck`() = testApplication {
        val services = startTestServer()
        seedDemoDeck(services)
        val admin = adminBrowser()

        val created = admin.post("/api/admin/punchlines") {
            contentType(ContentType.Application.Json)
            setBody(CardInput(packId = "demo-fr", text = "une carte ajoutée à la main", enabled = true))
        }.body<CardAdminView>()

        assertEquals("demo-fr", created.packId)
        val all = admin.get("/api/admin/punchlines").body<List<CardAdminView>>()
        assertTrue(all.any { it.id == created.id })
    }

    @Test
    fun `dropping the demo pack is refused while it still holds cards`() = testApplication {
        val services = startTestServer()
        seedDemoDeck(services)

        val response = adminBrowser().delete("/api/admin/packs/demo-fr")

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertEquals("PACK_NOT_EMPTY", response.body<ErrorResponse>().code)
    }

    @Test
    fun `a pack holding only disabled cards still refuses to be dropped`() = testApplication {
        val services = startTestServer()
        seedDemoDeck(services)
        val admin = adminBrowser()
        val situations = admin.get("/api/admin/situations").body<List<CardAdminView>>()
        situations.forEach { card ->
            admin.post("/api/admin/situations") {
                contentType(ContentType.Application.Json)
                setBody(CardInput(id = card.id, packId = card.packId, text = card.text, enabled = false))
            }
        }

        assertEquals(HttpStatusCode.Conflict, admin.delete("/api/admin/packs/demo-fr").status)
    }

    @Test
    fun `an empty pack can be dropped`() = testApplication {
        startTestServer()
        val admin = adminBrowser()
        val created = admin.post("/api/admin/packs") {
            contentType(ContentType.Application.Json)
            setBody(fr.ftnl.cardgame.api.dto.PackInput(name = "Vide", description = "", enabled = true))
        }.body<PackAdminView>()

        assertEquals(HttpStatusCode.NoContent, admin.delete("/api/admin/packs/${created.id}").status)
    }

    @Test
    fun `the seeder leaves an already filled catalogue alone`() = testApplication {
        val services = startTestServer()
        assertTrue(seedDemoDeck(services))

        assertTrue(!seedDemoDeck(services))
        val packs = adminBrowser().get("/api/admin/packs").body<List<PackAdminView>>()
        assertEquals(1, packs.size)
    }
}
