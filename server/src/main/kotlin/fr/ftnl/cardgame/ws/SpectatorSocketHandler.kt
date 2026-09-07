package fr.ftnl.cardgame.ws

import fr.ftnl.cardgame.api.view.GameViewFactory
import fr.ftnl.cardgame.domain.game.GameCode
import fr.ftnl.cardgame.game.GameService
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.json.Json

/**
 * Runs one spectator socket: the stream page, and the second monitor next to it.
 *
 * It is deliberately the poor cousin of [GameSocketHandler]. Nobody is announced online,
 * no seat is held or released, no grace delay is armed, and the only message it answers is
 * the ping the client uses to notice a dead link. Everything else is dropped on the floor:
 * a page whose whole point is that it cannot see the hand has no business sending
 * commands, and refusing them one by one would only tell a caller what to try next.
 */
class SpectatorSocketHandler(
    private val games: GameService,
    private val connections: GameConnections,
    private val views: GameViewFactory,
    private val json: Json,
) {

    suspend fun serve(session: WebSocketSession, code: GameCode) {
        val connection = SpectatorConnection(code, session, json)
        connections.add(connection)
        try {
            sendCurrentState(connection)
            session.incoming.consumeEach { frame -> onFrame(connection, frame) }
        } finally {
            connections.remove(connection)
        }
    }

    private suspend fun sendCurrentState(connection: SpectatorConnection) {
        val state = games.find(connection.code) ?: return
        connection.send(ServerMessage.State(views.spectate(state)))
    }

    private suspend fun onFrame(connection: SpectatorConnection, frame: Frame) {
        val text = (frame as? Frame.Text)?.readText() ?: return
        val message = runCatching { json.decodeFromString(ClientMessage.serializer(), text) }.getOrNull()
        if (message is ClientMessage.Ping) connection.send(ServerMessage.Pong)
    }
}
