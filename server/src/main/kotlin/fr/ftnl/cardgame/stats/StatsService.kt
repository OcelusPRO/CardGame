package fr.ftnl.cardgame.stats

import fr.ftnl.cardgame.api.dto.AdminOverview
import fr.ftnl.cardgame.api.dto.CardStatsView
import fr.ftnl.cardgame.api.dto.CardUsageView
import fr.ftnl.cardgame.api.dto.ComboView
import fr.ftnl.cardgame.api.dto.DailyActivityView
import fr.ftnl.cardgame.api.dto.LiveStatsView
import fr.ftnl.cardgame.catalog.CardPackRepository
import fr.ftnl.cardgame.catalog.PunchlineCardRepository
import fr.ftnl.cardgame.catalog.SituationCardRepository
import fr.ftnl.cardgame.domain.card.CardId
import fr.ftnl.cardgame.domain.game.GameClock
import fr.ftnl.cardgame.ws.GameConnections

/**
 * Read model of the administration dashboard. The counters come from the database, the
 * live numbers from the sockets currently open, and the card texts are joined in memory.
 */
class StatsService(
    private val reader: UsageStatsReader,
    private val packs: CardPackRepository,
    private val situations: SituationCardRepository,
    private val punchlines: PunchlineCardRepository,
    private val connections: GameConnections,
    private val clock: GameClock,
    private val days: DayProvider = DayProvider.UTC,
) {

    suspend fun overview(): AdminOverview = AdminOverview(
        packs = packs.all().size,
        situations = situations.count(),
        punchlines = punchlines.count(),
        live = live(),
        today = today(),
    )

    fun live(): LiveStatsView = LiveStatsView(
        activeGames = connections.activeGames(),
        connectedPlayers = connections.connectedPlayers(),
        timestampMillis = clock.nowMillis(),
    )

    suspend fun activity(days: Int): List<DailyActivityView> = reader.activity(days).map(::toActivityView)

    suspend fun topCards(kind: CardKind, limit: Int): List<CardUsageView> {
        val stats = reader.topCards(kind, limit)
        val texts = textsOf(kind, stats.map { it.cardId })
        return stats.map { stat ->
            CardUsageView(
                cardId = stat.cardId.value,
                kind = stat.kind.name,
                text = texts[stat.cardId].orEmpty(),
                deals = stat.deals,
                plays = stat.plays,
                votes = stat.votes,
                wins = stat.wins,
            )
        }
    }

    /** Everything the card lookup shows for one punchline: its counters and its best pairing. */
    suspend fun punchlineStats(cardId: String): CardStatsView {
        val id = CardId(cardId)
        val usage = reader.cardUsage(id, CardKind.PUNCHLINE)
        val text = textsOf(CardKind.PUNCHLINE, listOf(id))[id].orEmpty()
        val best = reader.bestSituationFor(id)
        val bestText = best
            ?.let { textsOf(CardKind.SITUATION, listOf(it.situationId))[it.situationId].orEmpty() }
        return CardStatsView(
            cardId = cardId,
            text = text,
            deals = usage?.deals ?: 0,
            plays = usage?.plays ?: 0,
            votes = usage?.votes ?: 0,
            wins = usage?.wins ?: 0,
            bestSituation = best?.let {
                CardStatsView.BestSituationView(
                    situationId = it.situationId.value,
                    text = bestText.orEmpty(),
                    plays = it.plays,
                    votes = it.votes,
                    wins = it.wins,
                )
            },
        )
    }

    suspend fun topCombos(limit: Int, minPlays: Int): List<ComboView> {
        val stats = reader.topCombos(limit, minPlays)
        val situationTexts = textsOf(CardKind.SITUATION, stats.map { it.situationId })
        val punchlineTexts = textsOf(CardKind.PUNCHLINE, stats.map { it.punchlineId })
        return stats.map { stat ->
            ComboView(
                situationId = stat.situationId.value,
                situationText = situationTexts[stat.situationId].orEmpty(),
                punchlineId = stat.punchlineId.value,
                punchlineText = punchlineTexts[stat.punchlineId].orEmpty(),
                plays = stat.plays,
                votes = stat.votes,
                wins = stat.wins,
                voteRatio = stat.voteRatio,
            )
        }
    }

    private suspend fun textsOf(kind: CardKind, ids: List<CardId>): Map<CardId, String> {
        if (ids.isEmpty()) return emptyMap()
        val wanted = ids.toSet()
        return when (kind) {
            CardKind.SITUATION -> situations.all().filter { it.id in wanted }.associate { it.id to it.text.raw }
            CardKind.PUNCHLINE -> punchlines.all().filter { it.id in wanted }.associate { it.id to it.text }
        }
    }

    private suspend fun today(): DailyActivityView {
        val today = days.today()
        return reader.activity(TODAY_WINDOW).firstOrNull { it.day == today }?.let(::toActivityView)
            ?: DailyActivityView(today, 0, 0, 0)
    }

    private fun toActivityView(activity: DailyActivity) = DailyActivityView(
        day = activity.day,
        gamesCreated = activity.gamesCreated,
        roundsPlayed = activity.roundsPlayed,
        answersPlayed = activity.answersPlayed,
    )

    private companion object {
        const val TODAY_WINDOW = 1
    }
}
