package fr.ftnl.cardgame.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

/** Liveness probe used by Docker Compose to wait for the server. */
fun Route.healthRoutes() {
    get("/api/health") {
        call.respondText("OK", status = HttpStatusCode.OK)
    }
}
