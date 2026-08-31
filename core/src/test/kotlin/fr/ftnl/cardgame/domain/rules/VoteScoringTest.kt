package fr.ftnl.cardgame.domain.rules

import fr.ftnl.cardgame.domain.game.GameSettings
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

class VoteScoringTest {

    private val scoring = VoteScoring()
    private val settings = GameFixtures.duoFriendly()
    private val alice = PlayerId("alice")
    private val bob = PlayerId("bob")
    private val carl = PlayerId("carl")
    private val dave = PlayerId("dave")

    private fun round(votes: Map<PlayerId, SubmissionId>, authors: List<PlayerId> = listOf(alice, bob, carl)) =
        Round(
            number = 1,
            situation = GameFixtures.situation("s1"),
            submissions = authors.associateWith { Submission(it, texts = listOf("réponse de $it")) },
            revealOrder = authors,
            votes = votes,
        )

    @Test
    fun `each vote is worth the configured number of points`() {
        val generous = settings.copy(scoring = ScoringSettings(pointsPerVote = 4, unanimityBonus = 0))
        val votes = mapOf(alice to SubmissionId(1), dave to SubmissionId(1), bob to SubmissionId(2))

        val outcome = scoring.score(round(votes), generous)

        assertEquals(8, outcome.points[bob])
        assertEquals(4, outcome.points[carl])
    }

    @Test
    fun `an answer everyone else picked earns the unanimity bonus`() {
        val votes = mapOf(bob to SubmissionId(0), carl to SubmissionId(0), alice to SubmissionId(1))

        val outcome = scoring.score(round(votes), settings)

        assertEquals(SubmissionId(0), outcome.topSubmission)
        assertEquals(2 + settings.scoring.unanimityBonus, outcome.points[alice])
    }

    @Test
    fun `one dissenting voter cancels the bonus entirely`() {
        val votes = mapOf(
            bob to SubmissionId(0),
            dave to SubmissionId(0),
            carl to SubmissionId(1),
            alice to SubmissionId(1),
        )

        val outcome = scoring.score(round(votes), settings)

        assertEquals(2, outcome.points[alice])
        assertEquals(2, outcome.points[bob])
        assertEquals(setOf(alice, bob), outcome.winners.toSet())
    }

    @Test
    fun `the author is left out of the count since they may not vote for themselves`() {
        // Alice wrote answer 0 and voted elsewhere; bob and carl are the only eligible voters.
        val votes = mapOf(bob to SubmissionId(0), carl to SubmissionId(0), alice to SubmissionId(2))

        assertEquals(SubmissionId(0), scoring.score(round(votes), settings).topSubmission)
    }

    @Test
    fun `with self voting on, the author must vote for their own answer too`() {
        val selfVote = settings.copy(allowSelfVote = true)
        val votes = mapOf(bob to SubmissionId(0), carl to SubmissionId(0), alice to SubmissionId(1))

        val outcome = scoring.score(round(votes), selfVote)

        assertEquals(2, outcome.points[alice])
        assertEquals(1, outcome.points[bob])
    }

    @Test
    fun `a bonus of zero turns unanimity into a plain vote count`() {
        val plain = settings.copy(scoring = ScoringSettings(unanimityBonus = 0))
        val votes = mapOf(bob to SubmissionId(0), carl to SubmissionId(0), alice to SubmissionId(1))

        assertEquals(2, scoring.score(round(votes), plain).points[alice])
    }

    @Test
    fun `an answer without a vote scores nothing`() {
        val outcome = scoring.score(round(mapOf(alice to SubmissionId(1))), settings)

        assertNull(outcome.points[carl])
        assertEquals(0, outcome.voteCounts[SubmissionId(2)])
    }

    @Test
    fun `at a table of two, each answer took the only vote it could`() {
        val duo = listOf(alice, bob)
        val votes = mapOf(alice to SubmissionId(1), bob to SubmissionId(0))

        val outcome = scoring.score(round(votes, duo), settings)

        val expected = 1 + settings.scoring.unanimityBonus
        assertEquals(expected, outcome.points[alice])
        assertEquals(expected, outcome.points[bob])
    }

    @Test
    fun `a round nobody voted in has no winner`() {
        val outcome = scoring.score(round(emptyMap()), settings)

        assertTrue(outcome.winners.isEmpty())
        assertTrue(outcome.points.isEmpty())
        assertNull(outcome.topSubmission)
    }
}
