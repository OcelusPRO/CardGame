package fr.ftnl.cardgame.domain.rules

import fr.ftnl.cardgame.domain.game.ChatVoteTally
import fr.ftnl.cardgame.domain.game.Round
import fr.ftnl.cardgame.domain.game.ScoringSettings
import fr.ftnl.cardgame.domain.game.Submission
import fr.ftnl.cardgame.domain.game.SubmissionId
import fr.ftnl.cardgame.domain.player.PlayerId
import fr.ftnl.cardgame.domain.support.GameFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** A round judged by the chats is worth one point, and nothing else. */
class ChatScoringTest {

    private val scoring = ChatScoring()
    private val settings = GameFixtures.duoFriendly()
    private val alice = PlayerId("alice")
    private val bob = PlayerId("bob")
    private val carl = PlayerId("carl")

    private fun round(chatVotes: Map<SubmissionId, Int>) = Round(
        number = 1,
        situation = GameFixtures.situation("s1"),
        submissions = listOf(alice, bob, carl).associateWith { Submission(it, texts = listOf("de $it")) },
        revealOrder = listOf(alice, bob, carl),
        chatVotes = chatVotes.mapValues { (_, count) -> ChatVoteTally(count) },
    )

    @Test
    fun `the most voted answer takes the round, and takes one point`() {
        val outcome = scoring.score(round(mapOf(SubmissionId(1) to 4_211, SubmissionId(0) to 12)), settings)

        assertEquals(listOf(bob), outcome.winners)
        assertEquals(mapOf(bob to 1), outcome.points)
        assertEquals(SubmissionId(1), outcome.topSubmission)
    }

    @Test
    fun `an audience of thousands is still worth exactly one point`() {
        val generous = settings.copy(scoring = ScoringSettings(pointsPerVote = 7, unanimityBonus = 9))

        val outcome = scoring.score(round(mapOf(SubmissionId(0) to 9_000)), generous)

        assertEquals(mapOf(alice to 1), outcome.points)
    }

    @Test
    fun `the counts are still shown as they were cast`() {
        val outcome = scoring.score(round(mapOf(SubmissionId(0) to 30, SubmissionId(2) to 5)), settings)

        assertEquals(30, outcome.voteCounts[SubmissionId(0)])
        assertEquals(0, outcome.voteCounts[SubmissionId(1)])
        assertEquals(5, outcome.voteCounts[SubmissionId(2)])
    }

    @Test
    fun `a tie at the top gives the point to each of them`() {
        val outcome = scoring.score(round(mapOf(SubmissionId(0) to 10, SubmissionId(2) to 10)), settings)

        assertEquals(setOf(alice, carl), outcome.winners.toSet())
        assertEquals(mapOf(alice to 1, carl to 1), outcome.points)
    }

    @Test
    fun `a round nobody voted in gives nothing to anybody`() {
        val outcome = scoring.score(round(emptyMap()), settings)

        assertTrue(outcome.winners.isEmpty())
        assertTrue(outcome.points.isEmpty())
        assertNull(outcome.topSubmission)
    }
}
