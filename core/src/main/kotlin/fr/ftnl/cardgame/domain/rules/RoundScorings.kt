package fr.ftnl.cardgame.domain.rules

import fr.ftnl.cardgame.domain.game.GameSettings
import fr.ftnl.cardgame.domain.game.SelectionMode

/**
 * Picks the scoring strategy matching the rules the host chose.
 *
 * The format comes first: a ladder pays by the duel whoever was voting in it, because what
 * it counts is duels won, not voices heard. Only when the answers were judged all at once
 * does it matter who did the judging.
 */
object RoundScorings {
    private val vote = VoteScoring()
    private val czar = CzarScoring()
    private val chat = ChatScoring()
    private val bracket = BracketScoring()

    fun of(settings: GameSettings): RoundScoring =
        if (settings.runsBracket) bracket else of(settings.selectionMode)

    private fun of(mode: SelectionMode): RoundScoring = when (mode) {
        SelectionMode.VOTE -> vote
        SelectionMode.CZAR -> czar
        SelectionMode.CHAT -> chat
    }
}
