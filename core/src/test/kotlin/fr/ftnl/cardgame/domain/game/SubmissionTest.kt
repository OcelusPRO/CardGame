package fr.ftnl.cardgame.domain.game

import fr.ftnl.cardgame.domain.card.CardId
import fr.ftnl.cardgame.domain.card.PunchlineCard
import fr.ftnl.cardgame.domain.player.PlayerId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SubmissionTest {

    private val alice = PlayerId("alice")
    private fun card(id: String, text: String) = PunchlineCard(CardId(id), text)

    @Test
    fun `a plain card submission reads back the card texts`() {
        val submission = Submission(alice, cards = listOf(card("p1", "un chat mouillé")))

        assertEquals(listOf("un chat mouillé"), submission.answers)
    }

    @Test
    fun `a hole card submission reads back the completed text`() {
        val submission = Submission(
            alice,
            cards = listOf(card("p1", "J'ai ____ dans mon ____")),
            cardFills = listOf(listOf("un chat", "sac")),
        )

        assertEquals(listOf("J'ai un chat dans mon sac"), submission.answers)
    }

    @Test
    fun `fills line up with the played cards, plain cards taking an empty slot`() {
        val submission = Submission(
            alice,
            cards = listOf(card("p1", "plain"), card("p2", "je veux ____")),
            cardFills = listOf(emptyList(), listOf("la paix")),
        )

        assertEquals(listOf("plain", "je veux la paix"), submission.answers)
    }

    @Test
    fun `card fills that do not line up with the cards are refused`() {
        assertFailsWith<IllegalArgumentException> {
            Submission(alice, cards = listOf(card("p1", "un")), cardFills = listOf(listOf("a"), listOf("b")))
        }
    }

    @Test
    fun `a blank card fill is refused`() {
        assertFailsWith<IllegalArgumentException> {
            Submission(alice, cards = listOf(card("p1", "je veux ____")), cardFills = listOf(listOf("  ")))
        }
    }
}
