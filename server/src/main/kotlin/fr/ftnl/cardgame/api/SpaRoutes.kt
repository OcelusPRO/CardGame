package fr.ftnl.cardgame.api

import fr.ftnl.cardgame.api.dto.ErrorResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.http.content.react
import io.ktor.server.http.content.singlePageApplication
import io.ktor.server.http.content.staticResources
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.route

/**
 * Serves the compiled single page application with Ktor's own SPA support: real files keep
 * their type, and anything else falls back on `index.html` so a deep link such as
 * `/rejoindre/ABCDE` survives a reload.
 *
 * The bundle and the API are carved out of that fallback on purpose. Answering `index.html`
 * to a request for a missing script is what turns a bad deployment into a blank page with
 * no error anywhere.
 */
fun Route.spaRoutes() {
    staticResources(ASSETS_PATH, "$STATIC_ROOT$ASSETS_PATH") {
        fallback { _, call -> call.respond(HttpStatusCode.NotFound, ErrorResponse("ASSET_NOT_FOUND")) }
    }

    route("/api/{...}") {
        handle { call.respond(HttpStatusCode.NotFound, ErrorResponse("NOT_FOUND")) }
    }

    singlePageApplication {
        useResources = true
        react(STATIC_ROOT)
    }
}

private const val STATIC_ROOT = "static"
private const val ASSETS_PATH = "/assets"
