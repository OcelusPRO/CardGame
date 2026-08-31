package fr.ftnl.cardgame.stats

import fr.ftnl.cardgame.domain.card.CardId
import fr.ftnl.cardgame.domain.card.CardOrigin
import fr.ftnl.cardgame.domain.card.PunchlineCard
import fr.ftnl.cardgame.domain.card.SituationCard
import fr.ftnl.cardgame.domain.card.SituationText
import fr.ftnl.cardgame.domain.engine.GameEvent
import fr.ftnl.cardgame.domain.game.Round
import fr.ftnl.cardgame.domain.game.RoundOutcome
import fr.ftnl.cardgame.domain.game.Submission
import fr.ftnl.cardgame.domain.game.SubmissionId
import fr.ftnl.cardgame.domain.player.PlayerId
import fr.ftnl.cardgame.support.RecordingStatsWriter
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StatsRecorderTest {

    private val writer = RecordingStatsWriter()
    private val recorder = StatsRecorder(writer)

    private val alice = PlayerId("alice")
    private val bob = PlayerId("bob")

    @Test
    fun `a finished round feeds the counters of every official card`() = runBlocking {
        recorder.onGameChanged(state = stateStub(), events = listOf(GameEvent.RoundEnded(round())))

        val usage = writer.rounds.single()
        assertEquals(CardId("s1"), usage.situationId)
        assertEquals(2, usage.answers.size)
        assertEquals(2, usage.answers.first { it.punchlineId == CardId("p1") }.votes)
        assertTrue(usage.answers.first { it.punchlineId == CardId("p1") }.won)
    }

    @Test
    fun `cards written by players never reach the database`() = runBlocking {
        val custom = round(
            punchlineOrigin = CardOrigin.CUSTOM,
            situation = SituationCard(CardId("c1"), SituationText("____ ?"), CardOrigin.CUSTOM),
        )

        recorder.onGameChanged(stateStub(), listOf(GameEvent.RoundEnded(custom)))

        val usage = writer.rounds.single()
        assertNull(usage.situationId)
        assertTrue(usage.answers.isEmpty())
    }

    @Test
    fun `cards drawn into a hand feed the deal counter`() = runBlocking {
        recorder.onGameChanged(
            stateStub(),
            listOf(GameEvent.HandsRefilled(listOf(CardId("p1"), CardId("p2"), CardId("p1")))),
        )

        assertEquals(listOf(CardId("p1"), CardId("p2"), CardId("p1")), writer.deals)
    }

    @Test
    fun `a refill that drew no official card records nothing`() = runBlocking {
        recorder.onGameChanged(stateStub(), listOf(GameEvent.HandsRefilled(emptyList())))

        assertTrue(writer.deals.isEmpty())
    }

    @Test
    fun `creating a game is counted once`() = runBlocking {
        recorder.onGameCreated(stateStub())

        assertEquals(1, writer.gamesCreated)
    }

    @Test
    fun `a round that was never scored is ignored`() = runBlocking {
        recorder.onGameChanged(stateStub(), listOf(GameEvent.RoundEnded(round().copy(outcome = null))))

        assertTrue(writer.rounds.isEmpty())
    }

    private fun round(
        punchlineOrigin: CardOrigin = CardOrigin.OFFICIAL,
        situation: SituationCard = SituationCard(CardId("s1"), SituationText("____ ?")),
    ): Round {
        val authors = listOf(alice, bob)
        return Round(
            number = 1,
            situation = situation,
            submissions = mapOf(
                alice to Submission(alice, cards = listOf(PunchlineCard(CardId("p1"), "un", punchlineOrigin))),
                bob to Submission(bob, cards = listOf(PunchlineCard(CardId("p2"), "deux", punchlineOrigin))),
            ),
            revealOrder = authors,
            outcome = RoundOutcome(
                points = mapOf(alice to 5),
                winners = listOf(alice),
                voteCounts = mapOf(SubmissionId(0) to 2, SubmissionId(1) to 0),
                topSubmission = SubmissionId(0),
            ),
        )
    }

    private fun stateStub() = fr.ftnl.cardgame.domain.game.GameState(
        code = fr.ftnl.cardgame.domain.game.GameCode.of("ABCDE"),
        hostId = alice,
    )
}
