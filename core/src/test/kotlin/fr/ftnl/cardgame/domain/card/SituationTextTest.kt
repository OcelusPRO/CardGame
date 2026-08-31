package fr.ftnl.cardgame.domain.card

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SituationTextTest {

    @Test
    fun `counts one blank when the sentence has no hole`() {
        assertEquals(1, SituationText("La meilleure excuse du lundi matin").blankCount)
    }

    @Test
    fun `counts every hole of the sentence`() {
        assertEquals(2, SituationText("____ plus ____, ça donne le chaos.").blankCount)
    }

    @Test
    fun `fills the holes in order`() {
        val text = SituationText("____ rencontre ____.")
        assertEquals("le chat rencontre la souris.", text.fill(listOf("le chat", "la souris")))
    }

    @Test
    fun `leaves a hole untouched when no answer is given for it`() {
        val text = SituationText("____ et ____.")
        assertEquals("le chat et ____.", text.fill(listOf("le chat")))
    }

    @Test
    fun `appends the answer when the sentence has no hole`() {
        val text = SituationText("Ma pire idée de vacances :")
        assertEquals("Ma pire idée de vacances : un camping en Sibérie", text.fill(listOf("un camping en Sibérie.")))
    }

    @Test
    fun `trims the trailing dot of an inlined answer`() {
        assertEquals("un chat mouillé arrive.", SituationText("____ arrive.").fill(listOf(" un chat mouillé. ")))
    }

    @Test
    fun `refuses a blank sentence`() {
        assertFailsWith<IllegalArgumentException> { SituationText("   ") }
    }
}
