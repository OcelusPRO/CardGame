package fr.ftnl.cardgame.support

import fr.ftnl.cardgame.api.dto.GameView
import fr.ftnl.cardgame.plugins.ApiJson
import fr.ftnl.cardgame.ws.ClientMessage
import fr.ftnl.cardgame.ws.ServerMessage
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.withTimeout

/** Sends a typed message on the game socket. */
suspend fun DefaultClientWebSocketSession.emit(message: ClientMessage) =
    send(ApiJson.encodeToString(ClientMessage.serializer(), message))

/**
 * Reads frames until a game snapshot matches [predicate]. Broadcasts pile up as players
 * connect, so a test must skip the ones it is not waiting for.
 */
suspend fun DefaultClientWebSocketSession.awaitState(
    timeoutMillis: Long = 10_000,
    predicate: (GameView) -> Boolean = { true },
): GameView = withTimeout(timeoutMillis) {
    while (true) {
        val message = nextMessage()
        if (message is ServerMessage.State && predicate(message.game)) return@withTimeout message.game
    }
    error("unreachable")
}

/** Reads frames until a refusal arrives, and returns its code. */
suspend fun DefaultClientWebSocketSession.awaitFailure(timeoutMillis: Long = 10_000): String =
    withTimeout(timeoutMillis) {
        while (true) {
            val message = nextMessage()
            if (message is ServerMessage.Failure) return@withTimeout message.code
        }
        error("unreachable")
    }

/**
 * Reads frames until the pong comes back. Frames are answered in order on one socket, so
 * a pong is also the proof that everything sent before it has been dealt with — which is
 * how a test waits for a message the server is meant to ignore.
 */
suspend fun DefaultClientWebSocketSession.awaitPong(timeoutMillis: Long = 10_000) =
    withTimeout(timeoutMillis) {
        while (true) {
            if (nextMessage() is ServerMessage.Pong) return@withTimeout
        }
    }

private suspend fun DefaultClientWebSocketSession.nextMessage(): ServerMessage? {
    val frame = incoming.receive() as? Frame.Text ?: return null
    return runCatching { ApiJson.decodeFromString(ServerMessage.serializer(), frame.readText()) }.getOrNull()
}
