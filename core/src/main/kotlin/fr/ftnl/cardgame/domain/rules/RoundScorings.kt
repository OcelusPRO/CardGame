package fr.ftnl.cardgame.domain.rules

import fr.ftnl.cardgame.domain.game.SelectionMode

/** Picks the scoring strategy matching the mode chosen by the host. */
object RoundScorings {
    private val vote = VoteScoring()
    private val czar = CzarScoring()
    private val chat = ChatScoring()

    fun of(mode: SelectionMode): RoundScoring = when (mode) {
        SelectionMode.VOTE -> vote
        SelectionMode.CZAR -> czar
        SelectionMode.CHAT -> chat
    }
}
