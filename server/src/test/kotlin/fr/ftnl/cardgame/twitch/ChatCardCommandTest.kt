package fr.ftnl.cardgame.twitch

import fr.ftnl.cardgame.domain.game.ChatCardAccess
import fr.ftnl.cardgame.domain.game.ChatCardSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Reading a card out of a chat line, and deciding whether the viewer paid for it.
 *
 * The gate is Twitch's own — the `bits` tag rides on the very message — so what is under
 * test here is that a claim is never taken on trust and that an ordinary sentence is never
 * mistaken for a proposal.
 */
class ChatCardCommandTest {

    @Test
    fun `a situation command becomes a situation`() {
        val proposal = ChatCardCommand.parse(line("!situation Le pire, c'est ____."))

        assertEquals(ChatCardKind.SITUATION, proposal?.kind)
        assertEquals("Le pire, c'est ____.", proposal?.text)
    }

    @Test
    fun `an answer command becomes a punchline, accents optional`() {
        assertEquals(ChatCardKind.PUNCHLINE, ChatCardCommand.parse(line("!réponse un chat mouillé"))?.kind)
        assertEquals(ChatCardKind.PUNCHLINE, ChatCardCommand.parse(line("!reponse un chat mouillé"))?.kind)
        assertEquals(ChatCardKind.PUNCHLINE, ChatCardCommand.parse(line("!PUNCHLINE un chat mouillé"))?.kind)
    }

    @Test
    fun `the short forms say the same thing as the long ones`() {
        assertEquals(ChatCardKind.SITUATION, ChatCardCommand.parse(line("!situ Le pire, c'est ____."))?.kind)
        assertEquals(ChatCardKind.PUNCHLINE, ChatCardCommand.parse(line("!rep un chat mouillé"))?.kind)
        assertEquals(ChatCardKind.PUNCHLINE, ChatCardCommand.parse(line("!carte un chat mouillé"))?.kind)
    }

    @Test
    fun `case and accents are the viewer's business, not the parser's`() {
        val spellings = listOf("!RÉPONSE", "!Réponse", "!rEpOnSe", "!REP", "!Rep")

        spellings.forEach { command ->
            assertEquals(
                ChatCardKind.PUNCHLINE,
                ChatCardCommand.parse(line("$command un chat mouillé"))?.kind,
                "$command should have been read as a punchline",
            )
        }
        assertEquals(ChatCardKind.SITUATION, ChatCardCommand.parse(line("!SITU une idée"))?.kind)
    }

    @Test
    fun `a space after the bang is a slip, not another command`() {
        val proposal = ChatCardCommand.parse(line("! rep un chat mouillé"))

        assertEquals(ChatCardKind.PUNCHLINE, proposal?.kind)
        assertEquals("un chat mouillé", proposal?.text)
    }

    @Test
    fun `a word that merely starts like the command is not the command`() {
        assertNull(ChatCardCommand.parse(line("!situationnisme une idée")))
        assertNull(ChatCardCommand.parse(line("!repartir en courant")))
        assertNull(ChatCardCommand.parse(line("!s une idée")))
    }

    @Test
    fun `somebody talking is not somebody proposing`() {
        assertNull(ChatCardCommand.parse(line("la situation est absurde mdr")))
        assertNull(ChatCardCommand.parse(line("j'aurais dit !reponse mais bon")))
        assertNull(ChatCardCommand.parse(line("rep un chat mouillé")))
    }

    @Test
    fun `an empty or oversized card is dropped rather than trimmed into nonsense`() {
        assertNull(ChatCardCommand.parse(line("!situation")))
        assertNull(ChatCardCommand.parse(line("!situation ab")))
        assertNull(ChatCardCommand.parse(line("!situation " + "a".repeat(ChatCardCommand.MAX_LENGTH + 1))))
    }

    @Test
    fun `the cheermotes Twitch glued on are not part of the card`() {
        val proposal = ChatCardCommand.parse(line("!situation Cheer100 Le pire, c'est ____.", bits = 100))

        assertEquals("Le pire, c'est ____.", proposal?.text)
    }

    @Test
    fun `a plain message keeps a word that merely looks like a cheermote`() {
        val proposal = ChatCardCommand.parse(line("!reponse le Falcon9 qui décolle"))

        assertEquals("le Falcon9 qui décolle", proposal?.text)
    }

    @Test
    fun `the free mode takes any line, the closed one takes none`() {
        assertTrue(ChatCardCommand.allows(rules(ChatCardAccess.EVERYONE), line("!situation une idée")))
        assertFalse(ChatCardCommand.allows(rules(ChatCardAccess.OFF), line("!situation une idée")))
    }

    @Test
    fun `bits mode wants a cheer, and a big enough one`() {
        val rules = rules(ChatCardAccess.BITS).copy(minBits = 100)

        assertFalse(ChatCardCommand.allows(rules, line("!situation gratuite")))
        assertFalse(ChatCardCommand.allows(rules, line("!situation trop peu", bits = 50)))
        assertTrue(ChatCardCommand.allows(rules, line("!situation payée", bits = 100)))
    }

    @Test
    fun `channel points never come through the chat, redemption tag or not`() {
        val points = rules(ChatCardAccess.CHANNEL_POINTS)

        // The game owns those rewards and reads them off EventSub, where a refused card
        // can hand the points back. A line in the room is somebody talking about it.
        assertFalse(ChatCardCommand.allows(points, line("!situation gratuite")))
        assertFalse(ChatCardCommand.allows(points, line("!situation payée", rewardId = "reward-42")))
    }

    @Test
    fun `a card written outside the chat carries no command to peel off`() {
        assertEquals("Le pire, c'est ____.", ChatCardCommand.cardOf("  Le pire,   c'est ____.  "))
        assertNull(ChatCardCommand.cardOf("ab"))
        assertNull(ChatCardCommand.cardOf("a".repeat(ChatCardCommand.MAX_LENGTH + 1)))
    }

    @Test
    fun `a pile the host closed refuses what is aimed at it`() {
        val rules = rules(ChatCardAccess.EVERYONE).copy(punchlines = false)

        assertTrue(ChatCardCommand.accepts(rules, ChatCardKind.SITUATION))
        assertFalse(ChatCardCommand.accepts(rules, ChatCardKind.PUNCHLINE))
    }

    private fun rules(access: ChatCardAccess) = ChatCardSettings(access = access)

    private fun line(text: String, bits: Int = 0, rewardId: String? = null) = ChatLine(
        channel = "kameto",
        viewerId = "1",
        viewerName = "Viewer",
        text = text,
        bits = bits,
        rewardId = rewardId,
    )
}
