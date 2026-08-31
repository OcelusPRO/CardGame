package fr.ftnl.cardgame.stats

import fr.ftnl.cardgame.domain.card.CardId

/** Queries the aggregated counters for the administration dashboard. */
interface UsageStatsReader {
    suspend fun topCards(kind: CardKind, limit: Int): List<CardUsageStat>
    suspend fun topCombos(limit: Int, minPlays: Int): List<ComboStat>
    suspend fun activity(days: Int): List<DailyActivity>

    /** The counters of one card, or null when it was never dealt. */
    suspend fun cardUsage(cardId: CardId, kind: CardKind): CardUsageStat?

    /** The situation this punchline scored best with, votes first, or null if never played. */
    suspend fun bestSituationFor(punchlineId: CardId): ComboStat?
}
