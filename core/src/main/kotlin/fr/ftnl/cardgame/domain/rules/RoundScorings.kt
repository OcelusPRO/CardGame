package fr.ftnl.cardgame.domain.rules

import fr.ftnl.cardgame.domain.game.SelectionMode

/** Picks the scoring strategy matching the mode chosen by the host. */
object RoundScorings {
    private val vote = VoteScoring()
    private val czar = CzarScoring()

    fun of(mode: SelectionMode): RoundScoring = when (mode) {
        // Chat mode is the vote with an empty table: the round holds no player vote, so
        // the very same counting leaves the chats as the only voices.
        SelectionMode.VOTE, SelectionMode.CHAT -> vote
        SelectionMode.CZAR -> czar
    }
}
