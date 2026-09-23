package fr.ftnl.cardgame.ws

import fr.ftnl.cardgame.api.view.GameViewFactory
import fr.ftnl.cardgame.auth.AdultAccessGuard
import fr.ftnl.cardgame.auth.PlayerSession
import fr.ftnl.cardgame.domain.engine.GameCommand
import fr.ftnl.cardgame.domain.engine.GameError
import fr.ftnl.cardgame.domain.game.GameCode
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.player.PlayerId
import fr.ftnl.cardgame.game.DispatchResult
import fr.ftnl.cardgame.game.GameService
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    private val adultAccess: AdultAccessGuard,
    private val scope: CoroutineScope,
    private val json: Json,
) {

    suspend fun serve(session: WebSocketSession, code: GameCode, identity: PlayerSession) {
        val playerId = PlayerId(identity.playerId)
        val connection = GameConnection(code, playerId, session, json)
        val allowAdult = adultAccess.allows(identity)
        connections.add(connection)
        try {
            // Signing in with Twitch is a full page redirect, so it usually happens once
            // the player is already seated: their account is refreshed on every socket.
            games.dispatch(
                code,
                GameCommand.LinkTwitch(
                    playerId,
                    identity.twitchLogin,
                    identity.twitchAvatarUrl,
                    identity.twitchId,
                ),
            )
            games.dispatch(code, GameCommand.SetConnected(playerId, connected = true))
            sendCurrentState(connection)
            session.incoming.consumeEach { frame -> onFrame(connection, frame, allowAdult) }
        } finally {
            connections.remove(connection)
            games.dispatch(code, GameCommand.SetConnected(playerId, connected = false))
            scheduleGraceDrop(code, playerId)
            forgetIfDeserted(code)
        }
    }

    /**
     * After a socket drops, the seat is held for a few seconds so a reconnection is
     * seamless. Once the delay is up, [GameCommand.DropIfAway] frees it — unless the
     * player came back, or the match is running (then the seat lives until the game ends).
     */
    private fun scheduleGraceDrop(code: GameCode, playerId: PlayerId) {
        scope.launch {
            delay(GRACE_MILLIS)
            games.dispatch(code, GameCommand.DropIfAway(playerId))
        }
    }

    /**
     * Once the last player is gone and nobody is left online, the table can go. Spectators
     * are not counted: a stream page left open all night is not somebody playing, and a
     * table it kept alive would outlive the game by hours.
     */
    private suspend fun forgetIfDeserted(code: GameCode) {
        if (connections.playersOf(code).isNotEmpty()) return
        val state = games.find(code) ?: return
        if (games.isAbandoned(state)) games.forget(code)
    }

    private suspend fun sendCurrentState(connection: GameConnection) {
        val state = games.find(connection.code) ?: return
        connection.send(ServerMessage.State(views.forDevice(state, connection.playerId, connection.seat)))
    }

    private suspend fun onFrame(connection: GameConnection, frame: Frame, allowAdult: Boolean) {
        val text = (frame as? Frame.Text)?.readText() ?: return
        val message = decode(text) ?: return connection.send(ServerMessage.Failure(BAD_MESSAGE))
        if (message is ClientMessage.Ping) return connection.send(ServerMessage.Pong)
        if (message is ClientMessage.Seat) return takeSeat(connection, message)
        run(connection, message, allowAdult)
    }

    /**
     * Passes a shared device to another player, or turns it back to the table. Only the
     * device that owns every seat may, and only to a seat that is actually there: on an
     * online table this would be a way to read somebody else's hand.
     */
    private suspend fun takeSeat(connection: GameConnection, message: ClientMessage.Seat) {
        val state = games.find(connection.code)
            ?: return connection.send(ServerMessage.Failure(GAME_NOT_FOUND))
        if (state.deviceOwner != connection.playerId) {
            return connection.send(ServerMessage.Failure(GameError.NOT_THE_HOST.name))
        }
        val seat = message.playerId?.let(::PlayerId)
        if (seat != null && !state.contains(seat)) {
            return connection.send(ServerMessage.Failure(GameError.UNKNOWN_PLAYER.name))
        }
        connection.seat = seat
        sendCurrentState(connection)
    }

    private suspend fun run(connection: GameConnection, message: ClientMessage, allowAdult: Boolean) {
        val state = games.find(connection.code)
            ?: return connection.send(ServerMessage.Failure(GAME_NOT_FOUND))
        val command = translator.toCommand(
            message,
            actorOf(connection, message),
            state.settings,
            connection.code,
            allowAdult,
            isHost = state.isHost(actorOf(connection, message)),
        ) ?: return
        when (val result = games.dispatch(connection.code, command)) {
            is DispatchResult.Refused -> connection.send(ServerMessage.Failure(result.error.name))
            DispatchResult.GameNotFound -> connection.send(ServerMessage.Failure(GAME_NOT_FOUND))
            is DispatchResult.Updated ->
                realignDeck(connection, before = state, after = result.state, allowAdult = allowAdult)
        }
    }

    /**
     * A change of answer mode can outlaw a pack still sitting in the live deck. The picker
     * only hides it; here the server actually rebuilds the pile so the game stays legal.
     */
    private suspend fun realignDeck(
        connection: GameConnection,
        before: GameState,
        after: GameState,
        allowAdult: Boolean,
    ) {
        if (before.settings.answerMode == after.settings.answerMode) return
        val rebuilt = translator.poolForMode(
            connection.code,
            connection.actor,
            after.settings.answerMode,
            allowAdult,
        ) ?: return
        games.dispatch(connection.code, rebuilt)
    }

    /**
     * Whose move a message is. It is the seat the socket holds, except for the two that
     * belong to the device itself: adding somebody to the sofa, and walking away with it.
     */
    private fun actorOf(connection: GameConnection, message: ClientMessage): PlayerId = when (message) {
        is ClientMessage.AddSeat, ClientMessage.Leave -> connection.playerId
        else -> connection.actor
    }

    private fun decode(text: String): ClientMessage? =
        runCatching { json.decodeFromString(ClientMessage.serializer(), text) }.getOrNull()

    private companion object {
        const val BAD_MESSAGE = "BAD_MESSAGE"
        const val GAME_NOT_FOUND = "GAME_NOT_FOUND"
        const val GRACE_MILLIS = 5_000L
    }
}
