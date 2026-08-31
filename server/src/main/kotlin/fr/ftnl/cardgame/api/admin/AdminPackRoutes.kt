package fr.ftnl.cardgame.api.admin

import fr.ftnl.cardgame.api.dto.DeckImportInput
import fr.ftnl.cardgame.api.dto.ErrorResponse
import fr.ftnl.cardgame.api.dto.PackInput
import fr.ftnl.cardgame.catalog.AdminPackService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

/** Pack management, reserved to the Discord accounts listed as administrators. */
fun Route.adminPackRoutes(packs: AdminPackService) {
    route("/api/admin/packs") {

        get {
            call.requireAdmin() ?: return@get
            call.respond(packs.all())
        }

        post {
            call.requireAdmin() ?: return@post
            call.respond(packs.save(call.receive<PackInput>()))
        }

        post("import") {
            call.requireAdmin() ?: return@post
            call.respond(packs.import(call.receive<DeckImportInput>()))
        }

        delete("{id}") {
            call.requireAdmin() ?: return@delete
            val id = call.parameters["id"].orEmpty()
            if (packs.delete(id)) call.respond(HttpStatusCode.NoContent)
            else call.respond(HttpStatusCode.NotFound, ErrorResponse("PACK_NOT_FOUND"))
        }
    }
}
