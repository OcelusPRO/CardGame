package fr.ftnl.cardgame.api.admin

import fr.ftnl.cardgame.api.dto.AdultAccessInput
import fr.ftnl.cardgame.api.dto.ErrorResponse
import fr.ftnl.cardgame.catalog.AdultAccessService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

/** The allowlist of accounts cleared for adult-only packs, reserved to admins. */
fun Route.adminAdultAccessRoutes(access: AdultAccessService) {
    route("/api/admin/adult-access") {

        get {
            call.requireAdmin() ?: return@get
            call.respond(access.all())
        }

        post {
            call.requireAdmin() ?: return@post
            call.respond(access.add(call.receive<AdultAccessInput>()))
        }

        delete("{provider}/{accountId}") {
            call.requireAdmin() ?: return@delete
            val provider = call.parameters["provider"].orEmpty()
            val id = call.parameters["accountId"].orEmpty()
            if (access.remove(provider, id)) call.respond(HttpStatusCode.NoContent)
            else call.respond(HttpStatusCode.NotFound, ErrorResponse("ADULT_ACCESS_NOT_FOUND"))
        }
    }
}
