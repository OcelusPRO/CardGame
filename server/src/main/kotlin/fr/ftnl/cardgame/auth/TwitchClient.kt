package fr.ftnl.cardgame.auth

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess

/**
 * Reads the profile of the player who just signed in with Twitch. Helix wants the client
 * id alongside the token, which is why this one is not a plain bearer call.
 */
class TwitchClient(private val http: HttpClient, private val clientId: String) {

    suspend fun me(accessToken: String): TwitchUser? {
        val response: HttpResponse = http.get(ME_URL) {
            bearerAuth(accessToken)
            header("Client-Id", clientId)
        }
        if (!response.status.isSuccess()) return null
        return response.body<TwitchUsers>().data.firstOrNull()
    }

    private companion object {
        const val ME_URL = "https://api.twitch.tv/helix/users"
    }
}
