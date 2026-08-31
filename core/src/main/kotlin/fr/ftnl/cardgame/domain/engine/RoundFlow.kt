package fr.ftnl.cardgame.domain.engine

import fr.ftnl.cardgame.domain.game.GamePhase
import fr.ftnl.cardgame.domain.game.GameState

/**
 * Drives a round from one step to the next. Steps close on their own as soon as
 * everybody has played, and the scheduler can force them when a timer expires.
 */
internal class RoundFlow(
    private val submissionCloser: SubmissionCloser,
    private val selectionCloser: SelectionCloser,
) {

    /** Advances as far as the answers collected so far allow. */
    fun advance(state: GameState, events: List<GameEvent>): CommandResult.Accepted {
        val afterAnswers =
            if (state.phase == GamePhase.SUBMITTING && RoundProgress.submissionsComplete(state)) {
                closeSubmissions(state, events)
            } else {
                CommandResult.Accepted(state, events)
            }
        return closeSelectionWhenDone(afterAnswers)
    }

    /** Ends the answering step even if some players did not play. */
    fun closeSubmissions(state: GameState, events: List<GameEvent>): CommandResult.Accepted =
        closeSelectionWhenDone(
            CommandResult.Accepted(submissionCloser.close(state), events + GameEvent.SubmissionsClosed)
        )

    /** Ends the selection step and reveals the score of the round. */
    fun closeSelection(state: GameState, events: List<GameEvent>): CommandResult.Accepted {
        val scored = selectionCloser.close(state)
        val round = scored.round ?: return CommandResult.Accepted(scored, events)
        return CommandResult.Accepted(scored, events + GameEvent.RoundEnded(round))
    }

    private fun closeSelectionWhenDone(current: CommandResult.Accepted): CommandResult.Accepted =
        if (current.state.phase == GamePhase.SELECTING && RoundProgress.selectionComplete(current.state)) {
            closeSelection(current.state, current.events)
        } else {
            current
        }
}
