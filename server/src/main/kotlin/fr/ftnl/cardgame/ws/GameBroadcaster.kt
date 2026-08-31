package fr.ftnl.cardgame.ws

import fr.ftnl.cardgame.api.view.GameViewFactory
import fr.ftnl.cardgame.domain.engine.GameEvent
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.game.GameListener

/** Pushes the new snapshot to every watcher, each one seeing only what they may see. */
class GameBroadcaster(
    private val connections: GameConnections,
    private val views: GameViewFactory,
) : GameListener {

    override suspend fun onGameChanged(state: GameState, events: List<GameEvent>) = broadcast(state)

    suspend fun broadcast(state: GameState) {
        connections.of(state.code).forEach { connection ->
            connection.send(ServerMessage.State(views.create(state, connection.playerId)))
        }
    }
}
