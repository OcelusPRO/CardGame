package fr.ftnl.cardgame.stats

import fr.ftnl.cardgame.domain.card.CardId

/** How one answer of a round performed. Custom cards carry no id and are never stored. */
data class PunchlineUsage(
    val punchlineId: CardId?,
    val votes: Int,
    val won: Boolean,
)
