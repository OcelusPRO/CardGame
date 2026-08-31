package fr.ftnl.cardgame.stats

import fr.ftnl.cardgame.domain.card.CardId

/** Raw counters of one situation and punchline pairing. */
data class ComboStat(
    val situationId: CardId,
    val punchlineId: CardId,
    val plays: Long,
    val votes: Long,
    val wins: Long,
) {
    /** Average number of votes the pairing collects each time it is played. */
    val voteRatio: Double get() = if (plays == 0L) 0.0 else votes.toDouble() / plays
}
