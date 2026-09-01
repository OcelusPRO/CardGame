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

/** The allowlist of Discord accounts cleared for adult-only packs, reserved to admins. */
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

        delete("{discordId}") {
            call.requireAdmin() ?: return@delete
            val id = call.parameters["discordId"].orEmpty()
            if (access.remove(id)) call.respond(HttpStatusCode.NoContent)
            else call.respond(HttpStatusCode.NotFound, ErrorResponse("ADULT_ACCESS_NOT_FOUND"))
        }
    }
}
