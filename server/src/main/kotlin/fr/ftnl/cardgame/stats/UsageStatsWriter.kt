package fr.ftnl.cardgame.stats

import fr.ftnl.cardgame.domain.card.CardId

/** Accumulates what happens in games into the aggregated counters. */
interface UsageStatsWriter {
    suspend fun recordGameCreated()
    suspend fun recordRound(usage: RoundUsage)

    /** One tick per official punchline freshly drawn into a hand. */
    suspend fun recordDeals(punchlineIds: List<CardId>)
}
