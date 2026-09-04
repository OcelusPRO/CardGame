package fr.ftnl.cardgame.twitch

import fr.ftnl.cardgame.domain.game.SubmissionId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** What a chat line has to look like to count as a vote. */
class ChatVoteTest {

    @Test
    fun `a bare number is the vote, counted from one on screen`() {
        assertEquals(SubmissionId(0), ChatVote.parse("1", answerCount = 3))
        assertEquals(SubmissionId(2), ChatVote.parse("3", answerCount = 3))
    }

    @Test
    fun `the usual chat decorations are accepted`() {
        listOf("!2", "#2", "vote 2", "!vote 2", "!vote #2", " 2 ", "VOTE 2").forEach {
            assertEquals(SubmissionId(1), ChatVote.parse(it, answerCount = 3), "on <$it>")
        }
    }

    @Test
    fun `somebody talking about an answer is not voting for it`() {
        assertNull(ChatVote.parse("la 2 est nulle", answerCount = 3))
        assertNull(ChatVote.parse("2 ou 3 ?", answerCount = 3))
        assertNull(ChatVote.parse("mdr", answerCount = 3))
    }

    @Test
    fun `a number pointing at nothing is not a vote`() {
        assertNull(ChatVote.parse("0", answerCount = 3))
        assertNull(ChatVote.parse("4", answerCount = 3))
        assertNull(ChatVote.parse("-1", answerCount = 3))
    }
}
