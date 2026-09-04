package fr.ftnl.cardgame.domain.rules

import fr.ftnl.cardgame.domain.game.Round
import fr.ftnl.cardgame.domain.game.ScoringSettings
import fr.ftnl.cardgame.domain.game.Submission
import fr.ftnl.cardgame.domain.game.SubmissionId
import fr.ftnl.cardgame.domain.player.PlayerId
import fr.ftnl.cardgame.domain.support.GameFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** What a Twitch chat is worth once the round is scored. */
class ChatVoteScoringTest {

    private val scoring = VoteScoring()
    private val settings = GameFixtures.duoFriendly()
    private val alice = PlayerId("alice")
    private val bob = PlayerId("bob")
    private val carl = PlayerId("carl")

    private fun round(
        votes: Map<PlayerId, SubmissionId> = emptyMap(),
        chatVotes: Map<String, Map<SubmissionId, Int>> = emptyMap(),
    ) = Round(
        number = 1,
        situation = GameFixtures.situation("s1"),
        submissions = listOf(alice, bob, carl).associateWith { Submission(it, texts = listOf("de $it")) },
        revealOrder = listOf(alice, bob, carl),
        votes = votes,
        chatVotes = chatVotes,
    )

    @Test
    fun `a chat speaks with a single voice, whatever its size`() {
        // Bonus off, so the count alone is under test: four thousand viewers, one voice.
        val plain = settings.copy(scoring = ScoringSettings(unanimityBonus = 0))
        val chat = mapOf("kameto" to mapOf(SubmissionId(0) to 4_211, SubmissionId(1) to 12))

        val outcome = scoring.score(round(chatVotes = chat), plain)

        assertEquals(1, outcome.voteCounts[SubmissionId(0)])
        assertEquals(plain.scoring.pointsPerVote, outcome.points[alice])
    }

    @Test
    fun `a chat left alone to judge can hand out the unanimity bonus`() {
        val chat = mapOf("kameto" to mapOf(SubmissionId(0) to 4_211))

        val outcome = scoring.score(round(chatVotes = chat), settings)

        assertEquals(
            settings.scoring.pointsPerVote + settings.scoring.unanimityBonus,
            outcome.points[alice],
        )
    }

    @Test
    fun `every watched chat brings its own voice`() {
        val chats = mapOf(
            "kameto" to mapOf(SubmissionId(1) to 300),
            "ponce" to mapOf(SubmissionId(1) to 2),
            "zerator" to mapOf(SubmissionId(0) to 90),
        )

        val outcome = scoring.score(round(chatVotes = chats), settings)

        assertEquals(2, outcome.voteCounts[SubmissionId(1)])
        assertEquals(listOf(bob), outcome.winners)
    }

    @Test
    fun `a chat that cannot make up its mind abstains`() {
        val split = mapOf("kameto" to mapOf(SubmissionId(0) to 50, SubmissionId(1) to 50))

        val outcome = scoring.score(round(chatVotes = split), settings)

        assertTrue(outcome.winners.isEmpty())
        assertEquals(0, outcome.voteCounts[SubmissionId(0)])
    }

    @Test
    fun `the chat adds up with the votes cast at the table`() {
        val chat = mapOf("kameto" to mapOf(SubmissionId(2) to 7))
        val votes = mapOf(alice to SubmissionId(2))

        val outcome = scoring.score(round(votes, chat), settings)

        assertEquals(2, outcome.voteCounts[SubmissionId(2)])
    }

    @Test
    fun `a chat voting elsewhere cancels the unanimity bonus`() {
        val votes = mapOf(bob to SubmissionId(0), carl to SubmissionId(0))
        val chat = mapOf("kameto" to mapOf(SubmissionId(1) to 40))

        val outcome = scoring.score(round(votes, chat), settings)

        assertEquals(2 * settings.scoring.pointsPerVote, outcome.points[alice])
    }

    @Test
    fun `a chat backing the table keeps the bonus`() {
        val votes = mapOf(bob to SubmissionId(0), carl to SubmissionId(0))
        val chat = mapOf("kameto" to mapOf(SubmissionId(0) to 40))

        val outcome = scoring.score(round(votes, chat), settings)

        assertEquals(3 * settings.scoring.pointsPerVote + settings.scoring.unanimityBonus, outcome.points[alice])
    }

    @Test
    fun `the tally shown on screen adds every chat together`() {
        val chats = mapOf(
            "kameto" to mapOf(SubmissionId(0) to 10),
            "ponce" to mapOf(SubmissionId(0) to 5, SubmissionId(1) to 1),
        )

        assertEquals(mapOf(SubmissionId(0) to 15, SubmissionId(1) to 1), round(chatVotes = chats).chatTally)
    }
}
