package fr.ftnl.cardgame.ws

import fr.ftnl.cardgame.domain.game.GameCode
import fr.ftnl.cardgame.domain.player.PlayerId
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.send
import kotlinx.serialization.json.Json

/** One open socket, tied to the player it authenticated as. */
class GameConnection(
    val code: GameCode,
    val playerId: PlayerId,
    private val session: WebSocketSession,
    private val json: Json,
) {
    /** Swallows a send failure: a dead socket is cleaned up by its own read loop. */
    suspend fun send(message: ServerMessage) {
        runCatching { session.send(json.encodeToString(ServerMessage.serializer(), message)) }
    }
}
