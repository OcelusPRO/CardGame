package fr.ftnl.cardgame.api.admin

import fr.ftnl.cardgame.api.dto.AccountView
import fr.ftnl.cardgame.api.dto.ErrorResponse
import fr.ftnl.cardgame.auth.AccountLookup
import fr.ftnl.cardgame.auth.AccountProvider
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

/**
 * Puts a name and a face on an account id — or, on Twitch, on a channel name — so the
 * administrator sees who they are about to clear before clearing them.
 */
fun Route.adminAccountRoutes(accounts: AccountLookup) {
    route("/api/admin/accounts") {

        get("{provider}/{query}") {
            call.requireAdmin() ?: return@get
            val provider = AccountProvider.ofOrNull(call.parameters["provider"])
                ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("UNKNOWN_PROVIDER"))
            val found = accounts.find(provider, call.parameters["query"].orEmpty())
                ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("ACCOUNT_NOT_FOUND"))
            call.respond(
                AccountView(
                    provider = found.provider.name,
                    accountId = found.id,
                    name = found.displayName,
                    login = found.login,
                    avatarUrl = found.avatarUrl,
                )
            )
        }
    }
}
