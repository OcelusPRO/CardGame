package fr.ftnl.cardgame.auth

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
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

    /**
     * Public profiles by account id, read with an application token: these are the
     * viewers who voted from a chat, and none of them ever signed in here. Helix takes a
     * hundred ids at a time and simply leaves out the ones it knows nothing about.
     */
    suspend fun viewers(appToken: String, ids: List<String>): List<TwitchUser> {
        if (ids.isEmpty()) return emptyList()
        val response: HttpResponse = http.get(ME_URL) {
            bearerAuth(appToken)
            header("Client-Id", clientId)
            ids.forEach { parameter("id", it) }
        }
        if (!response.status.isSuccess()) return emptyList()
        return response.body<TwitchUsers>().data
    }

    private companion object {
        const val ME_URL = "https://api.twitch.tv/helix/users"
    }
}
