package fr.ftnl.cardgame.domain.engine.handler

import fr.ftnl.cardgame.domain.engine.CommandResult
import fr.ftnl.cardgame.domain.engine.GameCommand
import fr.ftnl.cardgame.domain.engine.GameError
import fr.ftnl.cardgame.domain.engine.GameEvent
import fr.ftnl.cardgame.domain.engine.RoundStarter
import fr.ftnl.cardgame.domain.game.GamePhase
import fr.ftnl.cardgame.domain.game.GameState

/** Checks the table is ready, then deals the very first round. */
internal class StartHandler(private val roundStarter: RoundStarter) {

    fun handle(state: GameState, command: GameCommand.Start): CommandResult {
        if (!state.isHost(command.by)) return CommandResult.rejected(GameError.NOT_THE_HOST)
        if (state.phase != GamePhase.LOBBY) return CommandResult.rejected(GameError.WRONG_PHASE)
        if (state.connectedPlayers.size < state.settings.minPlayers) {
            return CommandResult.rejected(GameError.NOT_ENOUGH_PLAYERS)
        }
        deckError(state)?.let { return CommandResult.rejected(it) }
        val started = roundStarter.start(state, FIRST_ROUND)
        return CommandResult.accepted(
            started.state,
            GameEvent.GameStarted,
            GameEvent.RoundStarted(FIRST_ROUND),
            GameEvent.HandsRefilled(started.dealtPunchlines),
        )
    }

    /** The deck must hold a situation, and enough punchlines to fill every hand. */
    private fun deckError(state: GameState): GameError? = when {
        state.situations.size == 0 -> GameError.EMPTY_DECK
        !state.settings.dealsCards -> null
        state.punchlines.size < state.settings.handSize * state.connectedPlayers.size ->
            GameError.NOT_ENOUGH_CARDS

        else -> null
    }

    private companion object {
        const val FIRST_ROUND = 1
    }
}
