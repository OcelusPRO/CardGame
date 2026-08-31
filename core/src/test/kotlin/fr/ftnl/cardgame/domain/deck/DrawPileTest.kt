package fr.ftnl.cardgame.domain.deck

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DrawPileTest {

    private val pile = DrawPile(listOf("a", "b", "c"))

    @Test
    fun `draws from the top of the pile`() {
        val draw = pile.draw(2, IdentityShuffler)

        assertEquals(listOf("a", "b"), draw.cards)
        assertEquals(listOf("c"), draw.pile.available)
    }

    @Test
    fun `recycles the discard stack once the pile runs out`() {
        val played = pile.draw(3, IdentityShuffler).pile.discard(listOf("a", "b"))

        val draw = played.draw(2, IdentityShuffler)

        assertEquals(listOf("a", "b"), draw.cards)
        assertEquals(0, draw.pile.size)
    }

    @Test
    fun `returns what is left when the whole deck is too small`() {
        val draw = pile.draw(10, IdentityShuffler)

        assertEquals(listOf("a", "b", "c"), draw.cards)
        assertEquals(0, draw.pile.size)
    }

    @Test
    fun `drawing nothing leaves the pile untouched`() {
        assertEquals(pile, pile.draw(0, IdentityShuffler).pile)
    }

    @Test
    fun `refuses a negative draw`() {
        assertFailsWith<IllegalArgumentException> { pile.draw(-1, IdentityShuffler) }
    }

    @Test
    fun `shuffled keeps every card`() {
        val shuffled = DrawPile.shuffled(listOf("a", "b", "c"), RandomShuffler())

        assertEquals(3, shuffled.size)
        assertTrue(shuffled.available.containsAll(listOf("a", "b", "c")))
    }
}
