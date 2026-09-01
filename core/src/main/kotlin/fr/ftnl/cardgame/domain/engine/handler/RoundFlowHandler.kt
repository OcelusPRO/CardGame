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

    /**
     * Ends the match and, at the same time, frees the seats of everyone who is no longer
     * connected: their seat was only kept so they could reconnect while it was still being
     * played. The ranking is drawn from the players who actually stayed to the end.
     */
    private fun finish(state: GameState): CommandResult {
        val absent = state.players.filterNot { it.connected }.map { it.id }.toSet()
        val trimmed = state.withoutPlayers(absent)
            .copy(phase = GamePhase.FINISHED, phaseDeadlineMillis = null)
        val departures = absent.map { GameEvent.PlayerLeft(it) }
        return CommandResult.Accepted(
            trimmed,
            departures + GameEvent.GameEnded(GameEndCondition.winners(trimmed)),
        )
    }
}
