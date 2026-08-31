package fr.ftnl.cardgame.domain.card

import kotlin.test.Test
import kotlin.test.assertEquals

class CardPoolTest {

    private fun situation(id: String) = SituationCard(CardId(id), SituationText("____ !"))
    private fun punchline(id: String) = PunchlineCard(CardId(id), "texte $id")

    @Test
    fun `merges two pools`() {
        val official = CardPool(listOf(situation("s1")), listOf(punchline("p1")))
        val custom = CardPool(listOf(situation("s2")), listOf(punchline("p2")))

        val merged = official + custom

        assertEquals(listOf("s1", "s2"), merged.situations.map { it.id.value })
        assertEquals(listOf("p1", "p2"), merged.punchlines.map { it.id.value })
    }

    @Test
    fun `keeps the first card when an id is duplicated`() {
        val first = CardPool(listOf(situation("s1")), emptyList())
        val second = CardPool(listOf(SituationCard(CardId("s1"), SituationText("autre"))), emptyList())

        val merged = first + second

        assertEquals(1, merged.situations.size)
        assertEquals("____ !", merged.situations.single().text.raw)
    }
}
