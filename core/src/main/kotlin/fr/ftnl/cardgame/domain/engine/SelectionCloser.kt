package fr.ftnl.cardgame.domain.engine

import fr.ftnl.cardgame.domain.game.GameClock
import fr.ftnl.cardgame.domain.game.GamePhase
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.game.Round
import fr.ftnl.cardgame.domain.rules.RoundScorings

/** Scores the round, banks the points and sends every played card to the discard pile. */
internal class SelectionCloser(private val clock: GameClock) {

    fun close(state: GameState): GameState {
        val round = state.round ?: return state
        // A ladder closes a duel at a time. Only the one that crowns a champion ends the
        // judging; the others simply put the next pair on the table with a fresh timer.
        if (state.settings.runsBracket) {
            val advanced = round.settleDuel()
            if (!advanced.bracketSettled) return state.copy(round = advanced, phaseDeadlineMillis = nextDuel(state))
            return score(state.copy(round = advanced))
        }
        return score(state)
    }

    private fun score(state: GameState): GameState {
        val round = state.round ?: return state
        val outcome = RoundScorings.of(state.settings).score(round, state.settings)
        return state.copy(
            round = round.copy(outcome = outcome),
            scoreboard = state.scoreboard + outcome.points,
            punchlines = state.punchlines.discard(playedCards(round)),
            phase = GamePhase.ROUND_RESULT,
            phaseDeadlineMillis = clock.nowMillis() + state.settings.resultSeconds * MILLIS_PER_SECOND,
        )
    }

    private fun nextDuel(state: GameState): Long =
        clock.nowMillis() + state.settings.duelSeconds * MILLIS_PER_SECOND

    private fun playedCards(round: Round) = round.submissions.values.flatMap { it.cards }

    private companion object {
        const val MILLIS_PER_SECOND = 1000L
    }
}
