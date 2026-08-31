package fr.ftnl.cardgame

import fr.ftnl.cardgame.plugins.ApiJson
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json

/** The outgoing client, used only to read the Discord profile after a sign in. */
object HttpClientFactory {

    fun create(): HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(ApiJson)
        }
    }
}
