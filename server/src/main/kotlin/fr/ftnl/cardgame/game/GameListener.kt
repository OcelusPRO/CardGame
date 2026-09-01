package fr.ftnl.cardgame.game

import fr.ftnl.cardgame.domain.engine.GameEvent
import fr.ftnl.cardgame.domain.game.GameCode
import fr.ftnl.cardgame.domain.game.GameState

/**
 * Reacts to what happens in a game. Broadcasting, statistics and phase timers are all
 * plugged in this way, so [GameService] stays unaware of them.
 */
interface GameListener {

    suspend fun onGameCreated(state: GameState) = Unit

    suspend fun onGameChanged(state: GameState, events: List<GameEvent>) = Unit

    /** The game was dropped from the store; any per game resource can be released. */
    suspend fun onGameForgotten(code: GameCode) = Unit
}
