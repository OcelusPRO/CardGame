package fr.ftnl.cardgame.domain.engine

import fr.ftnl.cardgame.domain.deck.Shuffler
import fr.ftnl.cardgame.domain.game.GameClock
import fr.ftnl.cardgame.domain.game.GamePhase
import fr.ftnl.cardgame.domain.game.GameState

/**
 * Locks the answers in and shuffles them, so the reveal order carries no hint
 * about who played what.
 *
 * In [fr.ftnl.cardgame.domain.game.SelectionFormat.DUELS] that same shuffled order is the
 * seeding of the ladder, which is why the pairing gives nothing away either — and the step
 * that opens is one duel, not the whole judging.
 */
internal class SubmissionCloser(
    private val shuffler: Shuffler,
    private val clock: GameClock,
) {

    fun close(state: GameState): GameState {
        val round = state.round ?: return state
        val revealed = round.revealedInOrder(shuffler.shuffle(round.submissions.keys.toList()))
        return state.copy(
            round = if (state.settings.runsBracket) revealed.withBracket() else revealed,
            phase = GamePhase.SELECTING,
            phaseDeadlineMillis = clock.nowMillis() + state.settings.judgingSeconds * MILLIS_PER_SECOND,
        )
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1000L
    }
}
