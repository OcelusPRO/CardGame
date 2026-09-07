package fr.ftnl.cardgame.domain.engine

import fr.ftnl.cardgame.domain.card.CardId
import fr.ftnl.cardgame.domain.card.CardOrigin
import fr.ftnl.cardgame.domain.card.PunchlineCard
import fr.ftnl.cardgame.domain.card.SituationCard
import fr.ftnl.cardgame.domain.card.SituationText
import fr.ftnl.cardgame.domain.deck.DrawPile
import fr.ftnl.cardgame.domain.game.ChatCardAccess
import fr.ftnl.cardgame.domain.game.ChatCardSettings
import fr.ftnl.cardgame.domain.game.GamePhase
import fr.ftnl.cardgame.domain.game.GameSettings
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.support.GameFixtures
import fr.ftnl.cardgame.domain.support.eventsOf
import fr.ftnl.cardgame.domain.support.perform
import fr.ftnl.cardgame.domain.support.refusal
import fr.ftnl.cardgame.domain.support.testEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The viewers writing cards. Whether they paid for it is settled by the chat reader long
 * before this point; what the domain owns is whether the table is open to it at all, which
 * pile the card lands in, and the ceiling on how much of the game a chat may write.
 */
class GameEngineChatCardsTest {

    private val engine = testEngine()
    private val players = GameFixtures.players("alice", "bob")
    private val alice = players[0].id

    @Test
    fun `a closed table takes nothing from its chat`() {
        val refusal = engine.refusal(lobby(ChatCardAccess.OFF), GameCommand.AddChatCards(situations()))

        assertEquals(GameError.CHAT_CARDS_CLOSED, refusal)
    }

    @Test
    fun `an open table takes the situations its chat writes`() {
        val state = engine.perform(lobby(), GameCommand.AddChatCards(situations("un chat mouillé")))

        assertEquals(1, state.situations.size)
        assertEquals(CardOrigin.CHAT, state.situations.all.single().origin)
    }

    @Test
    fun `the piles the host closed stay closed`() {
        val rules = ChatCardSettings(access = ChatCardAccess.EVERYONE, situations = false)
        val open = lobby().let { it.copy(settings = it.settings.copy(chatCards = rules)) }

        val refusal = engine.refusal(open, GameCommand.AddChatCards(situations("nope")))

        assertEquals(GameError.CHAT_CARDS_CLOSED, refusal)
    }

    @Test
    fun `a punchline the chat wrote joins the pile the hands are dealt from`() {
        val state = engine.perform(lobby(), GameCommand.AddChatCards(punchlines = punchlines("la honte")))

        assertEquals(1, state.punchlines.size)
        assertEquals(CardOrigin.CHAT, state.punchlines.all.single().origin)
    }

    @Test
    fun `what the chat wrote is announced, so the table knows where it came from`() {
        val events = engine.eventsOf(
            lobby(),
            GameCommand.AddChatCards(situations("une"), punchlines("deux", "trois")),
        )

        val added = events.filterIsInstance<GameEvent.ChatCardsAdded>().single()
        assertEquals(1, added.situations)
        assertEquals(2, added.punchlines)
    }

    @Test
    fun `a chat can only write so much of one game`() {
        val many = (1..ChatCardSettings.MAX_CARDS_PER_GAME).map { "situation $it" }
        val full = engine.perform(lobby(), GameCommand.AddChatCards(situations(*many.toTypedArray())))

        assertEquals(ChatCardSettings.MAX_CARDS_PER_GAME, full.chatCardCount)
        assertEquals(
            GameError.CHAT_CARDS_FULL,
            engine.refusal(full, GameCommand.AddChatCards(situations("une de trop"))),
        )
    }

    @Test
    fun `what the chat writes is written down, so the host can keep it`() {
        val state = engine.perform(
            lobby(),
            GameCommand.AddChatCards(situations("une situation"), punchlines("une réponse")),
        )

        assertEquals(listOf("une situation"), state.chatCardLog.situations.map { it.text })
        assertEquals(listOf("une réponse"), state.chatCardLog.punchlines.map { it.text })
    }

    @Test
    fun `a card that has been dealt is still readable in what the chat wrote`() {
        val written =
            engine.perform(running(), GameCommand.AddChatCards(punchlines = punchlines("la honte")))

        val emptied = written.copy(punchlines = DrawPile(emptyList()))

        assertEquals(listOf("la honte"), emptied.chatCardLog.punchlines.map { it.text })
        assertEquals(1, emptied.chatCardCount)
    }

    @Test
    fun `a finished game takes nothing more`() {
        val over = lobby().copy(phase = GamePhase.FINISHED)

        assertEquals(
            GameError.WRONG_PHASE,
            engine.refusal(over, GameCommand.AddChatCards(situations("trop tard"))),
        )
    }

    @Test
    fun `cards written mid-game join the pile rather than jumping the queue`() {
        val running = running()
        val before = running.situations.available.map { it.id }

        val grown = engine.perform(running, GameCommand.AddChatCards(situations("tardive")))

        assertEquals(before.size + 1, grown.situations.available.size)
        assertTrue(
            grown.situations.available.map { it.id }.containsAll(before),
            "a card thrown in should not push the drawn ones out",
        )
    }

    private fun lobby(access: ChatCardAccess = ChatCardAccess.EVERYONE): GameState {
        val settings = GameSettings(minPlayers = 2, chatCards = ChatCardSettings(access = access))
        return GameFixtures.lobby(players, settings)
    }

    private fun running(): GameState {
        val dealt = engine.perform(lobby(), GameCommand.SetCardPool(alice, GameFixtures.pool()))
        return engine.perform(dealt, GameCommand.Start(alice))
    }

    private fun situations(vararg texts: String): List<SituationCard> =
        texts.mapIndexed { index, text ->
            SituationCard(CardId("chat-s-$index-$text"), SituationText(text), CardOrigin.CHAT)
        }

    private fun punchlines(vararg texts: String): List<PunchlineCard> =
        texts.mapIndexed { index, text ->
            PunchlineCard(CardId("chat-p-$index-$text"), text, CardOrigin.CHAT)
        }
}
