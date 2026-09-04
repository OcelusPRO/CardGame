package fr.ftnl.cardgame

import fr.ftnl.cardgame.plugins.ApiJson
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json

/**
 * The outgoing client: the Discord and Twitch profiles after a sign in, and the socket
 * the Twitch chats are read from.
 */
object HttpClientFactory {

    fun create(): HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(ApiJson)
        }
        install(WebSockets)
    }
}
