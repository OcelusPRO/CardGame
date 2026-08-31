package fr.ftnl.cardgame.stats

import fr.ftnl.cardgame.domain.card.CardId

/** Raw counters of a single card, without its text. */
data class CardUsageStat(
    val cardId: CardId,
    val kind: CardKind,
    val deals: Long,
    val plays: Long,
    val votes: Long,
    val wins: Long,
)
