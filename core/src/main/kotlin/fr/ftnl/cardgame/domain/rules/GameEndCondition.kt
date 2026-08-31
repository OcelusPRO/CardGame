package fr.ftnl.cardgame.domain.rules

import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.player.PlayerId

/** Decides whether a new round may start, and who eventually won. */
object GameEndCondition {

    /** Over once every planned situation has been played, or the deck ran dry first. */
    fun isReached(state: GameState): Boolean =
        hasPlayedEveryRound(state) || state.situations.size == 0

    /** The players tied at the best score. There is no target to reach, only a ranking. */
    fun winners(state: GameState): List<PlayerId> = state.scoreboard.leaders

    private fun hasPlayedEveryRound(state: GameState): Boolean =
        (state.round?.number ?: 0) >= state.settings.rounds
}
