package fr.ftnl.cardgame.domain.engine

import fr.ftnl.cardgame.domain.card.CardId
import fr.ftnl.cardgame.domain.game.GamePhase
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.support.GameFixtures
import fr.ftnl.cardgame.domain.support.perform
import fr.ftnl.cardgame.domain.support.testEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameEngineDisconnectionTest {

    private val engine = testEngine()
    private val players = GameFixtures.players("alice", "bob", "carl")
    private val alice = players[0].id
    private val bob = players[1].id
    private val carl = players[2].id

    private val running = startedGame()

    @Test
    fun `leaving a running game keeps the seat and the score`() {
        val state = engine.perform(running, GameCommand.Leave(carl))

        assertEquals(3, state.players.size)
        assertFalse(state.players.single { it.id == carl }.connected)
    }

    @Test
    fun `the round moves on once the missing player is offline`() {
        val answered = listOf(alice to "p1", bob to "p11").fold(running) { state, (player, card) ->
            engine.perform(state, GameCommand.PlayCards(player, listOf(CardId(card))))
        }

        val state = engine.perform(answered, GameCommand.SetConnected(carl, connected = false))

        assertEquals(GamePhase.SELECTING, state.phase)
    }

    @Test
    fun `coming back online restores the hand that was dealt`() {
        val gone = engine.perform(running, GameCommand.SetConnected(carl, connected = false))

        val back = engine.perform(gone, GameCommand.SetConnected(carl, connected = true))

        assertTrue(back.players.single { it.id == carl }.connected)
        assertEquals(running.handOf(carl), back.handOf(carl))
    }

    private fun startedGame(): GameState {
        val lobby = GameFixtures.lobby(players)
        val withCards = engine.perform(lobby, GameCommand.SetCardPool(alice, GameFixtures.pool()))
        return engine.perform(withCards, GameCommand.Start(alice))
    }
}
