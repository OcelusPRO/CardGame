package fr.ftnl.cardgame.domain.card

import kotlin.test.Test
import kotlin.test.assertEquals

class PunchlineCardTest {

    private fun card(text: String) = PunchlineCard(CardId("c1"), text)

    @Test
    fun `a plain card carries no hole`() {
        assertEquals(0, card("un chat mouillé").blankCount)
    }

    @Test
    fun `every run of underscores is one hole`() {
        assertEquals(2, card("J'ai ____ dans mon ____").blankCount)
    }

    @Test
    fun `filling drops the words into the holes in order`() {
        assertEquals(
            "J'ai un chat dans mon sac",
            card("J'ai ____ dans mon ____").fill(listOf("un chat", "sac")),
        )
    }

    @Test
    fun `a plain card ignores any fills handed to it`() {
        assertEquals("une pizza froide", card("une pizza froide").fill(listOf("ignoré")))
    }

    @Test
    fun `a hole with no fill is left as it is`() {
        assertEquals("J'ai un chat dans mon ____", card("J'ai ____ dans mon ____").fill(listOf("un chat")))
    }

    @Test
    fun `a fill is trimmed of blanks and of its trailing dot`() {
        assertEquals("j'adore les clowns", card("j'adore ____").fill(listOf("  les clowns. ")))
    }
}
