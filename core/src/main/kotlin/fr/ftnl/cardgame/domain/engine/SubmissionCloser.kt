package fr.ftnl.cardgame.domain.engine

import fr.ftnl.cardgame.domain.deck.Shuffler
import fr.ftnl.cardgame.domain.game.GameClock
import fr.ftnl.cardgame.domain.game.GamePhase
import fr.ftnl.cardgame.domain.game.GameState

/**
 * Locks the answers in and shuffles them, so the reveal order carries no hint
 * about who played what.
 */
internal class SubmissionCloser(
    private val shuffler: Shuffler,
    private val clock: GameClock,
) {

    fun close(state: GameState): GameState {
        val round = state.round ?: return state
        return state.copy(
            round = round.revealedInOrder(shuffler.shuffle(round.submissions.keys.toList())),
            phase = GamePhase.SELECTING,
            phaseDeadlineMillis = clock.nowMillis() + state.settings.selectSeconds * MILLIS_PER_SECOND,
        )
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1000L
    }
}
