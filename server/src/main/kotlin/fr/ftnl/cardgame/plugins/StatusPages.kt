package fr.ftnl.cardgame.plugins

import fr.ftnl.cardgame.api.dto.ErrorResponse
import fr.ftnl.cardgame.auth.UnknownAccountException
import fr.ftnl.cardgame.catalog.PackNotEmptyException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond

/** Turns every failure into the same small JSON body the client knows how to read. */
fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<PackNotEmptyException> { call, _ ->
            call.respond(HttpStatusCode.Conflict, ErrorResponse("PACK_NOT_EMPTY"))
        }
        exception<UnknownAccountException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, ErrorResponse("ACCOUNT_NOT_FOUND", cause.message))
        }
        exception<BadRequestException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("MALFORMED_REQUEST", cause.message))
        }
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("VALIDATION_ERROR", cause.message))
        }
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled failure on ${call.request.local.uri}", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("INTERNAL_ERROR"))
        }
    }
}
