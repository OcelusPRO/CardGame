package fr.ftnl.cardgame.api

import fr.ftnl.cardgame.api.dto.CardPackView
import fr.ftnl.cardgame.api.dto.PackAdminView
import fr.ftnl.cardgame.api.dto.PackInput
import fr.ftnl.cardgame.support.adminBrowser
import fr.ftnl.cardgame.support.browser
import fr.ftnl.cardgame.support.startTestServer
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A pack carrying a secret code is kept out of the lobby entirely: it only ever joins a
 * game when the host types that code into the situations box (see [fr.ftnl.cardgame.ws.GameSocketTest]).
 */
class SecretPackTest {

    private suspend fun createSecretPack(admin: HttpClient): PackAdminView =
        admin.post("/api/admin/packs") {
            contentType(ContentType.Application.Json)
            setBody(PackInput(name = "Deck caché", description = "", enabled = true, secretCode = "  licorne-42 "))
        }.body()

    @Test
    fun `a secret pack never shows up in the lobby, not even for an admin`() = testApplication {
        startTestServer()
        val admin = adminBrowser()
        val pack = createSecretPack(admin)

        assertFalse(
            browser().get("/api/packs").body<List<CardPackView>>().any { it.id == pack.id },
            "the anonymous lobby list must not leak a secret pack",
        )
        assertFalse(
            admin.get("/api/packs").body<List<CardPackView>>().any { it.id == pack.id },
            "even a cleared admin only reaches a secret pack through its code",
        )
    }

    @Test
    fun `the administration shows the pack with its trimmed code`() = testApplication {
        startTestServer()
        val admin = adminBrowser()
        val created = createSecretPack(admin)

        assertEquals("licorne-42", created.secretCode)
        val listed = admin.get("/api/admin/packs").body<List<PackAdminView>>().single { it.id == created.id }
        assertEquals("licorne-42", listed.secretCode)
    }

    @Test
    fun `clearing the code turns the pack back into a public one`() = testApplication {
        startTestServer()
        val admin = adminBrowser()
        val created = createSecretPack(admin)

        admin.post("/api/admin/packs") {
            contentType(ContentType.Application.Json)
            setBody(PackInput(id = created.id, name = created.name, description = "", enabled = true, secretCode = "   "))
        }

        val listed = admin.get("/api/admin/packs").body<List<PackAdminView>>().single { it.id == created.id }
        assertNull(listed.secretCode)
        assertTrue(admin.get("/api/packs").body<List<CardPackView>>().any { it.id == created.id })
    }
}
