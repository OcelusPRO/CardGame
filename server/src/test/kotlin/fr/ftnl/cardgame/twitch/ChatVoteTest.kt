package fr.ftnl.cardgame.twitch

import fr.ftnl.cardgame.domain.game.SubmissionId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** What a chat line has to look like to count as a vote, and what it ends up pointing at. */
class ChatVoteTest {

    /** A whole round on the table: three answers, numbered as they are shown. */
    private val wholeRound = listOf(SubmissionId(0), SubmissionId(1), SubmissionId(2))

    @Test
    fun `a bare number is the vote, counted from one on screen`() {
        assertEquals(SubmissionId(0), ChatVote.parse("1", wholeRound))
        assertEquals(SubmissionId(2), ChatVote.parse("3", wholeRound))
    }

    @Test
    fun `the usual chat decorations are accepted`() {
        listOf("!2", "#2", "vote 2", "!vote 2", "!vote #2", " 2 ", "VOTE 2").forEach {
            assertEquals(SubmissionId(1), ChatVote.parse(it, wholeRound), "on <$it>")
        }
    }

    @Test
    fun `somebody talking about an answer is not voting for it`() {
        assertNull(ChatVote.parse("la 2 est nulle", wholeRound))
        assertNull(ChatVote.parse("2 ou 3 ?", wholeRound))
        assertNull(ChatVote.parse("mdr", wholeRound))
    }

    @Test
    fun `a number pointing at nothing is not a vote`() {
        assertNull(ChatVote.parse("0", wholeRound))
        assertNull(ChatVote.parse("4", wholeRound))
        assertNull(ChatVote.parse("-1", wholeRound))
    }

    @Test
    fun `on a duel the numbers are the two cards on screen, not their ids`() {
        // A ladder can put answers 4 and 7 head to head; the chat still types 1 or 2.
        val duel = listOf(SubmissionId(4), SubmissionId(7))

        assertEquals(SubmissionId(4), ChatVote.parse("1", duel))
        assertEquals(SubmissionId(7), ChatVote.parse("2", duel))
        assertNull(ChatVote.parse("3", duel), "a duel only ever has two sides")
    }
}
