package fr.ftnl.cardgame.support

import fr.ftnl.cardgame.domain.card.CardId
import fr.ftnl.cardgame.stats.RoundUsage
import fr.ftnl.cardgame.stats.UsageStatsWriter

/** Captures what the recorder decided to store, without touching a database. */
class RecordingStatsWriter : UsageStatsWriter {
    var gamesCreated = 0
        private set
    val rounds = mutableListOf<RoundUsage>()
    val deals = mutableListOf<CardId>()

    override suspend fun recordGameCreated() {
        gamesCreated++
    }

    override suspend fun recordRound(usage: RoundUsage) {
        rounds += usage
    }

    override suspend fun recordDeals(punchlineIds: List<CardId>) {
        deals += punchlineIds
    }
}
