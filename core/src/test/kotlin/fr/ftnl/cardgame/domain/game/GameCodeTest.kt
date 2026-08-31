package fr.ftnl.cardgame.domain.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.random.Random

class GameCodeTest {

    @Test
    fun `normalises the code typed by a player`() {
        assertEquals("ABCDE", GameCode.of("  abcde ").value)
    }

    @Test
    fun `refuses a code of the wrong length`() {
        assertFailsWith<IllegalArgumentException> { GameCode.of("ABC") }
    }

    @Test
    fun `refuses look-alike characters`() {
        assertFailsWith<IllegalArgumentException> { GameCode.of("ABCD0") }
    }

    @Test
    fun `the alphabet holds no character that can be misread for another`() {
        val ambiguous = listOf('O', '0', 'I', '1', 'L', 'Q')

        ambiguous.forEach { character ->
            assertFalse(character in GameCode.ALPHABET, "$character can be misread and must go")
        }
    }

    @Test
    fun `ofOrNull swallows invalid input`() {
        assertNull(GameCode.ofOrNull("nope"))
    }

    @Test
    fun `generated codes always use the safe alphabet`() {
        val generator = RandomGameCodeGenerator(Random(42))

        repeat(200) {
            val code = generator.generate()
            assertEquals(GameCode.LENGTH, code.value.length)
            assertTrue(code.value.all { it in GameCode.ALPHABET })
        }
    }
}
