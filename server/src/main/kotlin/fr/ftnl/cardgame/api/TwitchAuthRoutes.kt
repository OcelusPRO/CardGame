package fr.ftnl.cardgame.api

import fr.ftnl.cardgame.auth.AdminGuard
import fr.ftnl.cardgame.auth.TwitchClient
import fr.ftnl.cardgame.auth.TwitchUser
import fr.ftnl.cardgame.auth.playerSession
import fr.ftnl.cardgame.plugins.TWITCH_PROVIDER
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
 * Optional Twitch sign in. It enriches the session with a name and a picture like Discord
 * does, hands the host the chat mode — a channel name is all the server needs to read a
 * chat — and opens the administration area for the allowlisted accounts.
 */
fun Route.twitchAuthRoutes(twitch: TwitchClient, guard: AdminGuard) {
    authenticate(TWITCH_PROVIDER) {

        get("/auth/twitch") {
            // Ktor redirects to Twitch before reaching this body.
        }

        get("/auth/twitch/callback") {
            val token = call.principal<OAuthAccessTokenResponse.OAuth2>()
                ?: return@get call.respondRedirect("/?twitch=refused")
            val user = twitch.me(token.accessToken)
                ?: return@get call.respondRedirect("/?twitch=failed")
            call.remember(user, guard)
            call.respondRedirect("/?twitch=ok")
        }
    }
}

private fun ApplicationCall.remember(user: TwitchUser, guard: AdminGuard) {
    sessions.set(
        playerSession().copy(
            twitchId = user.id,
            twitchLogin = user.login,
            twitchUsername = user.displayName,
            twitchAvatarUrl = user.profileImageUrl,
            twitchCreatedAtMillis = user.createdAtMillis,
        )
    )
    guard.sessionFor(user.account())?.let { sessions.set(it) }
}
