package fr.ftnl.cardgame.stats

import fr.ftnl.cardgame.db.dbQuery
import fr.ftnl.cardgame.db.table.CardUsageTable
import fr.ftnl.cardgame.db.table.ComboStatTable
import fr.ftnl.cardgame.db.table.DailyActivityTable
import fr.ftnl.cardgame.domain.card.CardId
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.LocalDate

/** Reads the aggregated counters, biggest first, for the administration charts. */
class ExposedUsageStatsReader(private val clock: DayProvider = DayProvider.UTC) : UsageStatsReader {

    override suspend fun topCards(kind: CardKind, limit: Int): List<CardUsageStat> = dbQuery {
        CardUsageTable.selectAll()
            .where { CardUsageTable.kind eq kind.name }
            .orderBy(CardUsageTable.plays to SortOrder.DESC)
            .limit(limit)
            .map(::toCardStat)
    }

    override suspend fun topCombos(limit: Int, minPlays: Int): List<ComboStat> = dbQuery {
        ComboStatTable.selectAll()
            .where { ComboStatTable.plays greaterEq minPlays.toLong() }
            .orderBy(ComboStatTable.votes to SortOrder.DESC)
            .limit(limit)
            .map(::toComboStat)
    }

    override suspend fun cardUsage(cardId: CardId, kind: CardKind): CardUsageStat? = dbQuery {
        CardUsageTable.selectAll()
            .where { (CardUsageTable.cardId eq cardId.value) and (CardUsageTable.kind eq kind.name) }
            .limit(1)
            .map(::toCardStat)
            .firstOrNull()
    }

    override suspend fun bestSituationFor(punchlineId: CardId): ComboStat? = dbQuery {
        ComboStatTable.selectAll()
            .where { ComboStatTable.punchlineId eq punchlineId.value }
            .orderBy(
                ComboStatTable.votes to SortOrder.DESC,
                ComboStatTable.wins to SortOrder.DESC,
                ComboStatTable.plays to SortOrder.DESC,
            )
            .limit(1)
            .map(::toComboStat)
            .firstOrNull()
    }

    override suspend fun activity(days: Int): List<DailyActivity> = dbQuery {
        val today = LocalDate.parse(clock.today())
        val start = today.minusDays((days - 1).toLong())
        val existingByDay = DailyActivityTable.selectAll()
            .where { DailyActivityTable.day greaterEq start.toString() }
            .map(::toActivity)
            .associateBy { it.day }
        generateSequence(start) { it.plusDays(1) }
            .takeWhile { !it.isAfter(today) }
            .map { date -> existingByDay[date.toString()] ?: DailyActivity(date.toString(), 0, 0, 0) }
            .toList()
    }

    private fun toCardStat(row: ResultRow) = CardUsageStat(
        cardId = CardId(row[CardUsageTable.cardId]),
        kind = CardKind.valueOf(row[CardUsageTable.kind]),
        deals = row[CardUsageTable.deals],
        plays = row[CardUsageTable.plays],
        votes = row[CardUsageTable.votes],
        wins = row[CardUsageTable.wins],
    )

    private fun toComboStat(row: ResultRow) = ComboStat(
        situationId = CardId(row[ComboStatTable.situationId]),
        punchlineId = CardId(row[ComboStatTable.punchlineId]),
        plays = row[ComboStatTable.plays],
        votes = row[ComboStatTable.votes],
        wins = row[ComboStatTable.wins],
    )

    private fun toActivity(row: ResultRow) = DailyActivity(
        day = row[DailyActivityTable.day],
        gamesCreated = row[DailyActivityTable.gamesCreated],
        roundsPlayed = row[DailyActivityTable.roundsPlayed],
        answersPlayed = row[DailyActivityTable.answersPlayed],
    )
}
