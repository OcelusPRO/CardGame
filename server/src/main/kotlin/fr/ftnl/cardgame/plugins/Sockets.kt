package fr.ftnl.cardgame.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import kotlin.time.Duration.Companion.seconds

/** WebSockets with a heartbeat, so a dropped connection is noticed quickly. */
fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 30.seconds
        maxFrameSize = MAX_FRAME_SIZE
        masking = false
    }
}

private const val MAX_FRAME_SIZE = 1L shl 20
