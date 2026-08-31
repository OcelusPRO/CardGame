package fr.ftnl.cardgame.ws

import fr.ftnl.cardgame.api.view.GameViewFactory
import fr.ftnl.cardgame.domain.engine.GameCommand
import fr.ftnl.cardgame.domain.game.GameCode
import fr.ftnl.cardgame.domain.player.PlayerId
import fr.ftnl.cardgame.game.DispatchResult
import fr.ftnl.cardgame.game.GameService
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.json.Json

/**
 * Runs one player socket: announce them online, push the current table, then turn every
 * incoming message into a command until the socket dies.
 */
class GameSocketHandler(
    private val games: GameService,
    private val connections: GameConnections,
    private val views: GameViewFactory,
    private val translator: GameCommandTranslator,
    private val json: Json,
) {

    suspend fun serve(session: WebSocketSession, code: GameCode, playerId: PlayerId) {
        val connection = GameConnection(code, playerId, session, json)
        connections.add(connection)
        try {
            games.dispatch(code, GameCommand.SetConnected(playerId, connected = true))
            sendCurrentState(connection)
            session.incoming.consumeEach { frame -> onFrame(connection, frame) }
        } finally {
            connections.remove(connection)
            games.dispatch(code, GameCommand.SetConnected(playerId, connected = false))
            forgetIfDeserted(code)
        }
    }

    /** Once the last watcher is gone and nobody is left online, the table can go. */
    private suspend fun forgetIfDeserted(code: GameCode) {
        if (connections.of(code).isNotEmpty()) return
        val state = games.find(code) ?: return
        if (games.isAbandoned(state)) games.forget(code)
    }

    private suspend fun sendCurrentState(connection: GameConnection) {
        val state = games.find(connection.code) ?: return
        connection.send(ServerMessage.State(views.create(state, connection.playerId)))
    }

    private suspend fun onFrame(connection: GameConnection, frame: Frame) {
        val text = (frame as? Frame.Text)?.readText() ?: return
        val message = decode(text) ?: return connection.send(ServerMessage.Failure(BAD_MESSAGE))
        if (message is ClientMessage.Ping) return connection.send(ServerMessage.Pong)
        run(connection, message)
    }

    private suspend fun run(connection: GameConnection, message: ClientMessage) {
        val state = games.find(connection.code)
            ?: return connection.send(ServerMessage.Failure(GAME_NOT_FOUND))
        val command = translator.toCommand(message, connection.playerId, state.settings) ?: return
        when (val result = games.dispatch(connection.code, command)) {
            is DispatchResult.Refused -> connection.send(ServerMessage.Failure(result.error.name))
            DispatchResult.GameNotFound -> connection.send(ServerMessage.Failure(GAME_NOT_FOUND))
            is DispatchResult.Updated -> Unit
        }
    }

    private fun decode(text: String): ClientMessage? =
        runCatching { json.decodeFromString(ClientMessage.serializer(), text) }.getOrNull()

    private companion object {
        const val BAD_MESSAGE = "BAD_MESSAGE"
        const val GAME_NOT_FOUND = "GAME_NOT_FOUND"
    }
}
