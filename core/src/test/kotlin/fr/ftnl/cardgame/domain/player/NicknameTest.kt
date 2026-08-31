package fr.ftnl.cardgame.domain.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class NicknameTest {

    @Test
    fun `trims and collapses whitespace`() {
        assertEquals("Jean Michel", Nickname.of("  Jean    Michel  ").value)
    }

    @Test
    fun `refuses a name that is too short`() {
        assertFailsWith<IllegalArgumentException> { Nickname.of("a") }
    }

    @Test
    fun `refuses a name that is too long`() {
        assertFailsWith<IllegalArgumentException> { Nickname.of("a".repeat(Nickname.MAX_LENGTH + 1)) }
    }

    @Test
    fun `ofOrNull swallows invalid input`() {
        assertNull(Nickname.ofOrNull(" "))
    }
}
