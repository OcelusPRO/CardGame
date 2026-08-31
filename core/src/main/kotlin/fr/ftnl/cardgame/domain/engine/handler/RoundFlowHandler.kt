package fr.ftnl.cardgame.domain.engine.handler

import fr.ftnl.cardgame.domain.engine.CommandResult
import fr.ftnl.cardgame.domain.engine.GameCommand
import fr.ftnl.cardgame.domain.engine.GameError
import fr.ftnl.cardgame.domain.engine.GameEvent
import fr.ftnl.cardgame.domain.engine.RoundFlow
import fr.ftnl.cardgame.domain.engine.RoundStarter
import fr.ftnl.cardgame.domain.game.GamePhase
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.rules.GameEndCondition

/** Handles the transitions triggered by a timer, or by the host asking for the next round. */
internal class RoundFlowHandler(
    private val roundFlow: RoundFlow,
    private val roundStarter: RoundStarter,
) {

    fun closeSubmissions(state: GameState): CommandResult =
        if (state.phase != GamePhase.SUBMITTING) CommandResult.rejected(GameError.WRONG_PHASE)
        else roundFlow.closeSubmissions(state, emptyList())

    fun closeSelection(state: GameState): CommandResult =
        if (state.phase != GamePhase.SELECTING) CommandResult.rejected(GameError.WRONG_PHASE)
        else roundFlow.closeSelection(state, emptyList())

    fun nextRound(state: GameState, command: GameCommand.NextRound): CommandResult {
        if (command.by != null && !state.isHost(command.by)) return CommandResult.rejected(GameError.NOT_THE_HOST)
        if (state.phase != GamePhase.ROUND_RESULT) return CommandResult.rejected(GameError.WRONG_PHASE)
        if (GameEndCondition.isReached(state)) return finish(state)
        val number = (state.round?.number ?: 0) + 1
        val started = roundStarter.start(state, number)
        return CommandResult.accepted(
            started.state,
            GameEvent.RoundStarted(number),
            GameEvent.HandsRefilled(started.dealtPunchlines),
        )
    }

    private fun finish(state: GameState): CommandResult = CommandResult.accepted(
        state.copy(phase = GamePhase.FINISHED, phaseDeadlineMillis = null),
        GameEvent.GameEnded(GameEndCondition.winners(state)),
    )
}
