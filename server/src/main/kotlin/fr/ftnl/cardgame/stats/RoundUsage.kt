package fr.ftnl.cardgame.stats

import fr.ftnl.cardgame.domain.card.CardId

/** What a finished round contributes to the statistics. */
data class RoundUsage(
    val situationId: CardId?,
    val answers: List<PunchlineUsage>,
)
