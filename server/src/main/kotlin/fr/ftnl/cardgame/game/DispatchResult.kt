package fr.ftnl.cardgame.game

import fr.ftnl.cardgame.domain.engine.GameError
import fr.ftnl.cardgame.domain.engine.GameEvent
import fr.ftnl.cardgame.domain.game.GameState

/** What running a command against a stored game produced. */
sealed interface DispatchResult {

    data class Updated(val state: GameState, val events: List<GameEvent>) : DispatchResult

    data class Refused(val error: GameError) : DispatchResult

    data object GameNotFound : DispatchResult
}
