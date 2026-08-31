package fr.ftnl.cardgame.api

import fr.ftnl.cardgame.auth.AdminGuard
import fr.ftnl.cardgame.auth.DiscordClient
import fr.ftnl.cardgame.auth.DiscordUser
import fr.ftnl.cardgame.auth.playerSession
import fr.ftnl.cardgame.plugins.DISCORD_PROVIDER
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.OAuthAccessTokenResponse
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set

/**
 * Optional Discord sign in. It only enriches the session with a name and a picture,
 * and opens the administration area for the allowlisted accounts.
 */
fun Route.discordAuthRoutes(discord: DiscordClient, guard: AdminGuard) {
    authenticate(DISCORD_PROVIDER) {

        get("/auth/discord") {
            // Ktor redirects to Discord before reaching this body.
        }

        get("/auth/discord/callback") {
            val token = call.principal<OAuthAccessTokenResponse.OAuth2>()
                ?: return@get call.respondRedirect("/?discord=refused")
            val user = discord.me(token.accessToken)
                ?: return@get call.respondRedirect("/?discord=failed")
            call.remember(user, guard)
            call.respondRedirect("/?discord=ok")
        }
    }
}

private fun ApplicationCall.remember(user: DiscordUser, guard: AdminGuard) {
    sessions.set(
        playerSession().copy(
            discordId = user.id,
            discordUsername = user.displayName,
            discordAvatarUrl = user.avatarUrl,
        )
    )
    guard.sessionFor(user)?.let { sessions.set(it) }
}
