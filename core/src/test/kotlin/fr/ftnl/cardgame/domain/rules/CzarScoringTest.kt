package fr.ftnl.cardgame.domain.rules

import fr.ftnl.cardgame.domain.game.Round
import fr.ftnl.cardgame.domain.game.Submission
import fr.ftnl.cardgame.domain.game.SubmissionId
import fr.ftnl.cardgame.domain.player.PlayerId
import fr.ftnl.cardgame.domain.support.GameFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CzarScoringTest {

    private val scoring = CzarScoring()
    private val settings = GameFixtures.duoFriendly()
    private val alice = PlayerId("alice")
    private val bob = PlayerId("bob")
    private val carl = PlayerId("carl")

    private fun round(votes: Map<PlayerId, SubmissionId>): Round {
        val authors = listOf(bob, carl)
        return Round(
            number = 1,
            situation = GameFixtures.situation("s1"),
            czarId = alice,
            submissions = authors.associateWith { Submission(it, texts = listOf("réponse de $it")) },
            revealOrder = authors,
            votes = votes,
        )
    }

    @Test
    fun `the picked answer wins the czar points`() {
        val outcome = scoring.score(round(mapOf(alice to SubmissionId(1))), settings)

        assertEquals(mapOf(carl to settings.scoring.pointsPerVote), outcome.points)
        assertEquals(listOf(carl), outcome.winners)
    }

    @Test
    fun `a vote from anyone but the czar is ignored`() {
        val outcome = scoring.score(round(mapOf(bob to SubmissionId(0))), settings)

        assertTrue(outcome.points.isEmpty())
    }

    @Test
    fun `the pick is worth the same as a vote`() {
        val generous = settings.copy(scoring = settings.scoring.copy(pointsPerVote = 5))

        val outcome = scoring.score(round(mapOf(alice to SubmissionId(1))), generous)

        assertEquals(5, outcome.points[carl])
    }

    @Test
    fun `a round the czar left unjudged scores nothing`() {
        val outcome = scoring.score(round(emptyMap()), settings)

        assertTrue(outcome.winners.isEmpty())
    }
}
