package fr.ftnl.cardgame.api.admin

import fr.ftnl.cardgame.api.dto.ErrorResponse
import fr.ftnl.cardgame.auth.AdminSession
import fr.ftnl.cardgame.auth.adminSession
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

/**
 * Returns the administrator behind the call, answering 403 when there is none.
 * Handlers stop with `?: return@get` so the guard is impossible to forget silently.
 */
suspend fun ApplicationCall.requireAdmin(): AdminSession? = adminSession()
    ?: run {
        respond(HttpStatusCode.Forbidden, ErrorResponse("ADMIN_REQUIRED"))
        null
    }
