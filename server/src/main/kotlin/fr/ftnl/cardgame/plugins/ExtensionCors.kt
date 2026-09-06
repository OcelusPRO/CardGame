package fr.ftnl.cardgame.plugins

import fr.ftnl.cardgame.config.TwitchExtensionConfig
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS

/**
 * The one cross origin exception this server makes.
 *
 * The whole site is same origin on purpose — the browser only ever talks to the host it
 * loaded the page from, which is why the session cookie needs no CORS at all. An extension
 * panel is the exception that proves it: it is served by Twitch, from an iframe on
 * `https://<extension id>.ext-twitch.tv`, and there is no other way for it to reach here.
 *
 * So the exception is spelled out to the character: those two hosts, the two methods the
 * panel uses, an `Authorization` header, and no credentials — the panel authenticates with
 * the token Twitch signed for it, never with a cookie of ours.
 */
fun Application.configureExtensionCors(config: TwitchExtensionConfig) {
    if (!config.enabled) return
    install(CORS) {
        allowHost("${config.clientId}.ext-twitch.tv", schemes = listOf("https"))
        // The rig a developer runs the panel in while building it.
        allowHost("localhost:8080", schemes = listOf("http"))
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowCredentials = false
    }
}
