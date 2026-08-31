package fr.ftnl.cardgame.api

import fr.ftnl.cardgame.api.dto.MeView
import fr.ftnl.cardgame.auth.AdminSession
import fr.ftnl.cardgame.auth.PlayerSession
import fr.ftnl.cardgame.auth.adminSession
import fr.ftnl.cardgame.auth.playerSession
import fr.ftnl.cardgame.config.DiscordConfig
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.sessions

/** Who am I, and how do I stop being it. */
fun Route.authRoutes(discord: DiscordConfig) {

    get("/api/me") {
        val player = call.playerSession()
        call.respond(
            MeView(
                playerId = player.playerId,
                discordConnected = player.discordId != null,
                discordUsername = player.discordUsername,
                discordAvatarUrl = player.discordAvatarUrl,
                isAdmin = call.adminSession() != null,
                discordLoginAvailable = discord.enabled,
            )
        )
    }

    post("/api/logout") {
        call.sessions.clear<PlayerSession>()
        call.sessions.clear<AdminSession>()
        call.respond(HttpStatusCode.NoContent)
    }
}
