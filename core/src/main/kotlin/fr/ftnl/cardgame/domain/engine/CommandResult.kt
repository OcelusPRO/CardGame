package fr.ftnl.cardgame.domain.engine

import fr.ftnl.cardgame.domain.game.GameState

/** Outcome of running a command: either a brand new snapshot, or a refusal. */
sealed interface CommandResult {

    data class Accepted(val state: GameState, val events: List<GameEvent>) : CommandResult

    data class Rejected(val error: GameError) : CommandResult

    companion object {
        fun accepted(state: GameState, vararg events: GameEvent): Accepted =
            Accepted(state, events.toList())

        fun rejected(error: GameError): Rejected = Rejected(error)
    }
}
