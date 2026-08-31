package fr.ftnl.cardgame.stats

import fr.ftnl.cardgame.db.dbQuery
import fr.ftnl.cardgame.db.table.CardUsageTable
import fr.ftnl.cardgame.db.table.ComboStatTable
import fr.ftnl.cardgame.db.table.DailyActivityTable
import fr.ftnl.cardgame.domain.card.CardId
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.update

/**
 * Increments the aggregated counters. Only official cards land here: cards written by
 * players stay in their own session and never reach the database.
 */
class ExposedUsageStatsWriter(private val days: DayProvider = DayProvider.UTC) : UsageStatsWriter {

    override suspend fun recordGameCreated(): Unit = dbQuery {
        bumpDay(gamesCreated = 1, roundsPlayed = 0, answersPlayed = 0)
    }

    override suspend fun recordDeals(punchlineIds: List<CardId>): Unit = dbQuery {
        punchlineIds.groupingBy { it }.eachCount().forEach { (cardId, times) ->
            bumpDeals(cardId, times.toLong())
        }
    }

    override suspend fun recordRound(usage: RoundUsage): Unit = dbQuery {
        usage.situationId?.let { bumpCard(it, CardKind.SITUATION, votes = 0, won = false) }
        usage.answers.forEach { answer ->
            val punchlineId = answer.punchlineId ?: return@forEach
            bumpCard(punchlineId, CardKind.PUNCHLINE, answer.votes, answer.won)
            usage.situationId?.let { bumpCombo(it, punchlineId, answer) }
        }
        bumpDay(gamesCreated = 0, roundsPlayed = 1, answersPlayed = usage.answers.size.toLong())
    }

    private fun bumpDeals(cardId: CardId, times: Long) {
        val updated = CardUsageTable.update(
            { (CardUsageTable.cardId eq cardId.value) and (CardUsageTable.kind eq CardKind.PUNCHLINE.name) }
        ) { row ->
            row[deals] = deals + times
        }
        if (updated > 0) return
        CardUsageTable.insert { row ->
            row[CardUsageTable.cardId] = cardId.value
            row[kind] = CardKind.PUNCHLINE.name
            row[deals] = times
        }
    }

    private fun bumpCard(cardId: CardId, kind: CardKind, votes: Int, won: Boolean) {
        val updated = CardUsageTable.update(
            { (CardUsageTable.cardId eq cardId.value) and (CardUsageTable.kind eq kind.name) }
        ) { row ->
            row[plays] = plays + 1
            row[CardUsageTable.votes] = CardUsageTable.votes + votes.toLong()
            row[wins] = wins + if (won) 1L else 0L
        }
        if (updated > 0) return
        CardUsageTable.insert { row ->
            row[CardUsageTable.cardId] = cardId.value
            row[CardUsageTable.kind] = kind.name
            row[plays] = 1
            row[CardUsageTable.votes] = votes.toLong()
            row[wins] = if (won) 1L else 0L
        }
    }

    private fun bumpCombo(situation: CardId, punchline: CardId, answer: PunchlineUsage) {
        val updated = ComboStatTable.update(
            {
                (ComboStatTable.situationId eq situation.value) and
                    (ComboStatTable.punchlineId eq punchline.value)
            }
        ) { row ->
            row[plays] = plays + 1
            row[votes] = votes + answer.votes.toLong()
            row[wins] = wins + if (answer.won) 1L else 0L
        }
        if (updated > 0) return
        ComboStatTable.insert { row ->
            row[situationId] = situation.value
            row[punchlineId] = punchline.value
            row[plays] = 1
            row[votes] = answer.votes.toLong()
            row[wins] = if (answer.won) 1L else 0L
        }
    }

    private fun bumpDay(gamesCreated: Long, roundsPlayed: Long, answersPlayed: Long) {
        val today = days.today()
        val updated = DailyActivityTable.update({ DailyActivityTable.day eq today }) { row ->
            row[DailyActivityTable.gamesCreated] = DailyActivityTable.gamesCreated + gamesCreated
            row[DailyActivityTable.roundsPlayed] = DailyActivityTable.roundsPlayed + roundsPlayed
            row[DailyActivityTable.answersPlayed] = DailyActivityTable.answersPlayed + answersPlayed
        }
        if (updated > 0) return
        DailyActivityTable.insert { row ->
            row[day] = today
            row[DailyActivityTable.gamesCreated] = gamesCreated
            row[DailyActivityTable.roundsPlayed] = roundsPlayed
            row[DailyActivityTable.answersPlayed] = answersPlayed
        }
    }
}
