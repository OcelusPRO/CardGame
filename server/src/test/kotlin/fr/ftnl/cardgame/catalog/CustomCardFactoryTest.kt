package fr.ftnl.cardgame.catalog

import fr.ftnl.cardgame.domain.card.CardOrigin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CustomCardFactoryTest {

    private val factory = CustomCardFactory()

    @Test
    fun `blank and duplicated texts are dropped`() {
        val cards = factory.punchlines(listOf(" un ", "un", "   ", "deux"))

        assertEquals(listOf("un", "deux"), cards.map { it.text })
    }

    @Test
    fun `an oversized text is dropped rather than truncated`() {
        assertTrue(factory.punchlines(listOf("a".repeat(500))).isEmpty())
    }

    @Test
    fun `custom cards are marked as such so statistics ignore them`() {
        val cards = factory.situations(listOf("Chez moi, ____."))

        assertTrue(cards.all { it.origin == CardOrigin.CUSTOM })
    }

    @Test
    fun `every custom card gets its own identifier`() {
        val cards = factory.punchlines(listOf("un", "deux", "trois"))

        assertEquals(3, cards.map { it.id }.distinct().size)
    }
}
