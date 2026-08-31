package fr.ftnl.cardgame.domain.engine

import fr.ftnl.cardgame.domain.card.CardId
import fr.ftnl.cardgame.domain.card.CardPool
import fr.ftnl.cardgame.domain.card.PunchlineCard
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.support.GameFixtures
import fr.ftnl.cardgame.domain.support.perform
import fr.ftnl.cardgame.domain.support.refusal
import fr.ftnl.cardgame.domain.support.testEngine
import kotlin.test.Test
import kotlin.test.assertEquals

/** Punchline cards that carry their own holes: the player completes them as they play. */
class GameEngineBlankPunchlineTest {

    private val engine = testEngine()
    private val players = GameFixtures.players("alice", "bob")
    private val alice = players[0].id
    private val bob = players[1].id

    private val running = startedGame()

    @Test
    fun `a hole card is played with its completed text`() {
        val state = engine.perform(
            running,
            GameCommand.PlayCards(alice, listOf(CardId("hole")), fills = listOf(listOf("un chat", "sac"))),
        )

        assertEquals(listOf("J'ai un chat dans le sac"), state.round?.submissions?.get(alice)?.answers)
    }

    @Test
    fun `too few words for the holes is refused`() {
        val refusal = engine.refusal(
            running,
            GameCommand.PlayCards(alice, listOf(CardId("hole")), fills = listOf(listOf("un chat"))),
        )

        assertEquals(GameError.WRONG_BLANK_COUNT, refusal)
    }

    @Test
    fun `a hole card played without any words is refused`() {
        val refusal = engine.refusal(running, GameCommand.PlayCards(alice, listOf(CardId("hole"))))

        assertEquals(GameError.WRONG_BLANK_COUNT, refusal)
    }

    @Test
    fun `a plain card is still played without any words`() {
        val plain = running.handOf(bob).first { it.blankCount == 0 }

        val state = engine.perform(running, GameCommand.PlayCards(bob, listOf(plain.id)))

        assertEquals(listOf(plain.text), state.round?.submissions?.get(bob)?.answers)
    }

    private fun startedGame(): GameState {
        val pool = CardPool(
            situations = (1..4).map { GameFixtures.situation("s$it") },
            punchlines = listOf(PunchlineCard(CardId("hole"), "J'ai ____ dans le ____")) +
                (1..40).map { GameFixtures.punchline("p$it") },
        )
        val withCards = engine.perform(
            GameFixtures.lobby(players),
            GameCommand.SetCardPool(alice, pool),
        )
        return engine.perform(withCards, GameCommand.Start(alice))
    }
}
