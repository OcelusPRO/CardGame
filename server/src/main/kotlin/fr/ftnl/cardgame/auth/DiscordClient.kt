package fr.ftnl.cardgame.auth

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess

/** Reads the profile of the player who just signed in with Discord. */
class DiscordClient(private val http: HttpClient) {

    suspend fun me(accessToken: String): DiscordUser? {
        val response: HttpResponse = http.get(ME_URL) { bearerAuth(accessToken) }
        return if (response.status.isSuccess()) response.body<DiscordUser>() else null
    }

    /**
     * The public profile behind an id. Discord only answers a bot for this, so it needs
     * `DISCORD_BOT_TOKEN`; without it there is simply no way to ask, and null comes back.
     */
    suspend fun user(botToken: String, id: String): DiscordUser? {
        if (botToken.isBlank() || id.isBlank()) return null
        val response: HttpResponse = http.get("$USERS_URL/$id") {
            header(HttpHeaders.Authorization, "Bot $botToken")
        }
        return if (response.status.isSuccess()) response.body<DiscordUser>() else null
    }

    private companion object {
        const val ME_URL = "https://discord.com/api/users/@me"
        const val USERS_URL = "https://discord.com/api/users"
    }
}
