package fr.ftnl.cardgame.auth

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess

/** Reads the profile of the player who just signed in with Discord. */
class DiscordClient(private val http: HttpClient) {

    suspend fun me(accessToken: String): DiscordUser? {
        val response: HttpResponse = http.get(ME_URL) { bearerAuth(accessToken) }
        return if (response.status.isSuccess()) response.body<DiscordUser>() else null
    }

    private companion object {
        const val ME_URL = "https://discord.com/api/users/@me"
    }
}
