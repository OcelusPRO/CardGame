package fr.ftnl.cardgame.api

import fr.ftnl.cardgame.api.dto.ErrorResponse
import fr.ftnl.cardgame.api.dto.MeView
import fr.ftnl.cardgame.support.browser
import fr.ftnl.cardgame.support.startTestServer
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class AdminRoutesTest {

    @Test
    fun `the administration is closed without a Discord session`() = testApplication {
        startTestServer()

        val response = browser().get("/api/admin/stats/overview")

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertEquals("ADMIN_REQUIRED", response.body<ErrorResponse>().code)
    }

    @Test
    fun `editing the catalogue is closed too`() = testApplication {
        startTestServer()

        assertEquals(HttpStatusCode.Forbidden, browser().get("/api/admin/situations").status)
        assertEquals(HttpStatusCode.Forbidden, browser().get("/api/admin/packs").status)
    }

    @Test
    fun `an anonymous visitor still gets a player identity`() = testApplication {
        startTestServer()

        val me = browser().get("/api/me").body<MeView>()

        assertFalse(me.isAdmin)
        assertFalse(me.discordConnected)
        assertFalse(me.discordLoginAvailable)
    }

    @Test
    fun `the health probe answers`() = testApplication {
        startTestServer()

        assertEquals(HttpStatusCode.OK, browser().get("/api/health").status)
    }
}
