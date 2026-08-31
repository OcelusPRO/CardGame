package fr.ftnl.cardgame.api

import io.ktor.http.URLProtocol
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.origin

/** The public origin of this request, used to build the link behind the QR code. */
fun ApplicationCall.baseUrl(): String {
    val origin = request.origin
    val defaultPort = if (origin.scheme == URLProtocol.HTTPS.name) 443 else 80
    val port = if (origin.serverPort == defaultPort) "" else ":${origin.serverPort}"
    return "${origin.scheme}://${origin.serverHost}$port"
}
