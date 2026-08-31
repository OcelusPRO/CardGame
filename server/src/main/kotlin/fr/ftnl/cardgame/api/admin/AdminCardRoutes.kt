package fr.ftnl.cardgame.api.admin

import fr.ftnl.cardgame.api.dto.CardInput
import fr.ftnl.cardgame.api.dto.ErrorResponse
import fr.ftnl.cardgame.catalog.AdminCardService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

/** Card management for both decks, reserved to administrators. */
fun Route.adminCardRoutes(cards: AdminCardService) {
    route("/api/admin/situations") {
        get {
            call.requireAdmin() ?: return@get
            call.respond(cards.allSituations())
        }
        post {
            call.requireAdmin() ?: return@post
            call.respond(cards.saveSituation(call.receive<CardInput>()))
        }
        delete("{id}") {
            call.requireAdmin() ?: return@delete
            call.respondDeletion(cards.deleteSituation(call.parameters["id"].orEmpty()))
        }
    }

    route("/api/admin/punchlines") {
        get {
            call.requireAdmin() ?: return@get
            call.respond(cards.allPunchlines())
        }
        post {
            call.requireAdmin() ?: return@post
            call.respond(cards.savePunchline(call.receive<CardInput>()))
        }
        delete("{id}") {
            call.requireAdmin() ?: return@delete
            call.respondDeletion(cards.deletePunchline(call.parameters["id"].orEmpty()))
        }
    }
}

private suspend fun io.ktor.server.application.ApplicationCall.respondDeletion(deleted: Boolean) =
    if (deleted) respond(HttpStatusCode.NoContent)
    else respond(HttpStatusCode.NotFound, ErrorResponse("CARD_NOT_FOUND"))
