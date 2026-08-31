package fr.ftnl.cardgame.api

import fr.ftnl.cardgame.support.browser
import fr.ftnl.cardgame.support.startTestServer
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The bundle must be served as a bundle. Answering `index.html` to a script request is
 * exactly what turns the whole site into a blank page, so it is checked here.
 */
class SpaRoutesTest {

    @Test
    fun `a bundle file is served as javascript and not as the index page`() = testApplication {
        startTestServer()

        val response = browser().get("/assets/probe.js")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(ContentType.Text.JavaScript, response.contentType()?.withoutParameters())
        assertTrue(response.bodyAsText().contains("spa-asset-ok"))
    }

    @Test
    fun `a missing bundle file answers 404 rather than the index page`() = testApplication {
        startTestServer()

        val response = browser().get("/assets/nope.js")

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(!response.bodyAsText().contains("<div id=\"root\">"))
    }

    @Test
    fun `the root serves the index page`() = testApplication {
        startTestServer()

        val response = browser().get("/")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(ContentType.Text.Html, response.contentType()?.withoutParameters())
        assertTrue(response.bodyAsText().contains("<div id=\"root\">"))
    }

    @Test
    fun `a deep link falls back on the index page so the router can take over`() = testApplication {
        startTestServer()

        val response = browser().get("/rejoindre/ABCDE")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("<div id=\"root\">"))
    }

    @Test
    fun `an unknown API path stays a JSON 404`() = testApplication {
        startTestServer()

        val response = browser().get("/api/nope")

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("NOT_FOUND"))
    }
}
