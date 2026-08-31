package fr.ftnl.cardgame.db

import fr.ftnl.cardgame.domain.card.CardId
import fr.ftnl.cardgame.stats.CardKind
import fr.ftnl.cardgame.stats.DayProvider
import fr.ftnl.cardgame.stats.ExposedUsageStatsReader
import fr.ftnl.cardgame.stats.ExposedUsageStatsWriter
import fr.ftnl.cardgame.stats.PunchlineUsage
import fr.ftnl.cardgame.stats.RoundUsage
import fr.ftnl.cardgame.support.TestDatabase
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class UsageStatsTest {

    private val today = DayProvider { "2026-08-31" }
    private val writer = ExposedUsageStatsWriter(today)
    private val reader = ExposedUsageStatsReader()

    @BeforeTest
    fun setUp() {
        TestDatabase.connect()
    }

    @Test
    fun `counters add up across rounds`() = runBlocking {
        repeat(3) { writer.recordRound(round(votes = 2, won = true)) }

        val stat = reader.topCards(CardKind.PUNCHLINE, limit = 10).single()

        assertEquals(3, stat.plays)
        assertEquals(6, stat.votes)
        assertEquals(3, stat.wins)
    }

    @Test
    fun `landing in a hand bumps the deal counter, apart from the play counter`() = runBlocking {
        writer.recordDeals(listOf(CardId("p1"), CardId("p1"), CardId("p2")))
        writer.recordRound(round(votes = 1, won = false))

        val stats = reader.topCards(CardKind.PUNCHLINE, limit = 10).associateBy { it.cardId }

        assertEquals(2, stats.getValue(CardId("p1")).deals)
        assertEquals(1, stats.getValue(CardId("p1")).plays)
        assertEquals(1, stats.getValue(CardId("p2")).deals)
        assertEquals(0, stats.getValue(CardId("p2")).plays)
    }

    @Test
    fun `the pairing of a situation and a punchline is tracked`() = runBlocking {
        writer.recordRound(round(votes = 4, won = true))
        writer.recordRound(round(votes = 0, won = false))

        val combo = reader.topCombos(limit = 10, minPlays = 1).single()

        assertEquals(2, combo.plays)
        assertEquals(4, combo.votes)
        assertEquals(2.0, combo.voteRatio)
    }

    @Test
    fun `a pairing below the play threshold is filtered out`() = runBlocking {
        writer.recordRound(round(votes = 1, won = false))

        assertEquals(0, reader.topCombos(limit = 10, minPlays = 5).size)
    }

    @Test
    fun `daily activity accumulates games and rounds`() = runBlocking {
        writer.recordGameCreated()
        writer.recordRound(round(votes = 1, won = true))
        writer.recordRound(round(votes = 1, won = false))

        val day = reader.activity(days = 7).single()

        assertEquals("2026-08-31", day.day)
        assertEquals(1, day.gamesCreated)
        assertEquals(2, day.roundsPlayed)
        assertEquals(2, day.answersPlayed)
    }

    @Test
    fun `an answer written by a player is skipped without breaking the round`() = runBlocking {
        writer.recordRound(RoundUsage(CardId("s1"), listOf(PunchlineUsage(null, votes = 3, won = true))))

        val situations = reader.topCards(CardKind.SITUATION, limit = 10)

        assertEquals(1, situations.size)
        assertEquals(0, reader.topCards(CardKind.PUNCHLINE, limit = 10).size)
    }

    private fun round(votes: Int, won: Boolean) =
        RoundUsage(CardId("s1"), listOf(PunchlineUsage(CardId("p1"), votes, won)))
}
