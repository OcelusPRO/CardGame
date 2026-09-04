package fr.ftnl.cardgame.domain.rules

import fr.ftnl.cardgame.domain.game.ChatVoteTally
import fr.ftnl.cardgame.domain.game.ChatVoter
import fr.ftnl.cardgame.domain.game.Round
import fr.ftnl.cardgame.domain.game.ScoringSettings
import fr.ftnl.cardgame.domain.game.Submission
import fr.ftnl.cardgame.domain.game.SubmissionId
import fr.ftnl.cardgame.domain.player.PlayerId
import fr.ftnl.cardgame.domain.support.GameFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** What a Twitch chat is worth once the round is scored: one voice per viewer. */
class ChatVoteScoringTest {

    private val scoring = VoteScoring()
    private val settings = GameFixtures.duoFriendly()
    private val alice = PlayerId("alice")
    private val bob = PlayerId("bob")
    private val carl = PlayerId("carl")

    private fun round(
        votes: Map<PlayerId, SubmissionId> = emptyMap(),
        chatVotes: Map<SubmissionId, ChatVoteTally> = emptyMap(),
    ) = Round(
        number = 1,
        situation = GameFixtures.situation("s1"),
        submissions = listOf(alice, bob, carl).associateWith { Submission(it, texts = listOf("de $it")) },
        revealOrder = listOf(alice, bob, carl),
        votes = votes,
        chatVotes = chatVotes,
    )

    private fun viewers(count: Int, faces: Int = 0) = ChatVoteTally(
        count = count,
        voters = (1..minOf(faces, count)).map { ChatVoter("v$it", "Viewer $it") },
    )

    @Test
    fun `every viewer counts for one voice`() {
        // Bonus off, so the count alone is under test.
        val plain = settings.copy(scoring = ScoringSettings(unanimityBonus = 0))
        val chat = mapOf(SubmissionId(0) to viewers(240), SubmissionId(1) to viewers(12))

        val outcome = scoring.score(round(chatVotes = chat), plain)

        assertEquals(240, outcome.voteCounts[SubmissionId(0)])
        assertEquals(240 * plain.scoring.pointsPerVote, outcome.points[alice])
        assertEquals(12 * plain.scoring.pointsPerVote, outcome.points[bob])
    }

    @Test
    fun `the chat picks the winner when it is the only judge`() {
        val chat = mapOf(SubmissionId(1) to viewers(300), SubmissionId(0) to viewers(90))

        assertEquals(listOf(bob), scoring.score(round(chatVotes = chat), settings).winners)
    }

    @Test
    fun `viewers and players add up on the same answer`() {
        val outcome = scoring.score(
            round(votes = mapOf(alice to SubmissionId(2)), chatVotes = mapOf(SubmissionId(2) to viewers(7))),
            settings,
        )

        assertEquals(8, outcome.voteCounts[SubmissionId(2)])
    }

    @Test
    fun `a single viewer voting elsewhere cancels the unanimity bonus`() {
        val votes = mapOf(bob to SubmissionId(0), carl to SubmissionId(0))
        val chat = mapOf(SubmissionId(0) to viewers(40), SubmissionId(1) to viewers(1))

        val outcome = scoring.score(round(votes, chat), settings)

        assertEquals(42 * settings.scoring.pointsPerVote, outcome.points[alice])
    }

    @Test
    fun `a chat unanimously behind the table keeps the bonus`() {
        val votes = mapOf(bob to SubmissionId(0), carl to SubmissionId(0))
        val chat = mapOf(SubmissionId(0) to viewers(40))

        val outcome = scoring.score(round(votes, chat), settings)

        assertEquals(
            42 * settings.scoring.pointsPerVote + settings.scoring.unanimityBonus,
            outcome.points[alice],
        )
    }

    @Test
    fun `an answer no viewer picked scores nothing`() {
        val outcome = scoring.score(round(chatVotes = mapOf(SubmissionId(0) to viewers(3))), settings)

        assertEquals(0, outcome.voteCounts[SubmissionId(2)])
        assertTrue(carl !in outcome.points)
    }

    @Test
    fun `a round nobody judged has no winner`() {
        val outcome = scoring.score(round(), settings)

        assertTrue(outcome.winners.isEmpty())
        assertTrue(outcome.points.isEmpty())
    }

    @Test
    fun `only the faces the table shows are carried, the rest is a number`() {
        val tally = viewers(count = 4_211, faces = ChatVoteTally.MAX_FACES)

        assertEquals(4_211, tally.count)
        assertEquals(15, tally.voters.size)
    }
}
