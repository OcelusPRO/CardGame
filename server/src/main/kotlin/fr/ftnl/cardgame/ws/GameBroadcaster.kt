package fr.ftnl.cardgame.ws

import fr.ftnl.cardgame.api.view.GameViewFactory
import fr.ftnl.cardgame.domain.engine.GameEvent
import fr.ftnl.cardgame.domain.game.GameCode
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.game.GameListener
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** Pushes the new snapshot to every watcher, each one seeing only what they may see. */
class GameBroadcaster(
    private val connections: GameConnections,
    private val views: GameViewFactory,
) : GameListener {

    override suspend fun onGameChanged(state: GameState, events: List<GameEvent>) = broadcast(state)

    /** A dropped table takes its stream pages with it; the seats close on their own. */
    override suspend fun onGameForgotten(code: GameCode) =
        connections.closeSpectators(code, "Cette partie est terminée")

    /**
     * Every socket is written to in parallel. Sending suspends until the client actually
     * takes the frame, so a phone on a struggling network would otherwise hold the whole
     * table behind it — the answer of the fastest player would land when the slowest one
     * caught up.
     */
    suspend fun broadcast(state: GameState): Unit = coroutineScope {
        connections.of(state.code).forEach { connection ->
            launch { connection.send(ServerMessage.State(snapshotFor(connection, state))) }
        }
    }

    private fun snapshotFor(connection: TableConnection, state: GameState) = when (connection) {
        is GameConnection -> views.create(state, connection.playerId)
        is SpectatorConnection -> views.spectate(state)
    }
}
