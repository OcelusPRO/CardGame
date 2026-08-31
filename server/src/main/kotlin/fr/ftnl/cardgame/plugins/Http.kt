package fr.ftnl.cardgame.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.compression.deflate
import io.ktor.server.plugins.compression.gzip
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import org.slf4j.event.Level

/** Transport level concerns: compression, standard headers and request logging. */
fun Application.configureHttp() {
    install(DefaultHeaders)
    install(Compression) {
        gzip()
        deflate()
    }
    install(CallLogging) {
        level = Level.INFO
        filter { call -> !call.request.local.uri.startsWith("/assets") }
    }
}
