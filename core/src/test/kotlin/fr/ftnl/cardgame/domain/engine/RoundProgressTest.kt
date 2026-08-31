package fr.ftnl.cardgame.domain.engine

import fr.ftnl.cardgame.domain.card.CardId
import fr.ftnl.cardgame.domain.game.GameSettings
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.game.SelectionMode
import fr.ftnl.cardgame.domain.support.GameFixtures
import fr.ftnl.cardgame.domain.support.perform
import fr.ftnl.cardgame.domain.support.testEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RoundProgressTest {

    private val engine = testEngine()
    private val players = GameFixtures.players("alice", "bob", "carl")
    private val alice = players[0].id
    private val bob = players[1].id
    private val carl = players[2].id

    @Test
    fun `lists the players we are still waiting an answer from`() {
        val running = startedGame(SelectionMode.VOTE)

        val state = engine.perform(running, GameCommand.PlayCards(alice, listOf(CardId("p1"))))

        assertEquals(setOf(bob, carl), RoundProgress.pendingAnswers(state).toSet())
        assertFalse(RoundProgress.submissionsComplete(state))
    }

    @Test
    fun `everybody may vote when several answers are on the table`() {
        val answered = everybodyAnswers(startedGame(SelectionMode.VOTE))

        assertEquals(setOf(alice, bob, carl), RoundProgress.voters(answered).toSet())
    }

    @Test
    fun `a player with only their own answer on the table is not a voter`() {
        val running = startedGame(SelectionMode.VOTE)
        val alone = engine.perform(running, GameCommand.PlayCards(alice, listOf(CardId("p1"))))
        val closed = engine.perform(alone, GameCommand.CloseSubmissions)

        assertTrue(alice !in RoundProgress.voters(closed))
    }

    @Test
    fun `self voting makes every connected player a voter`() {
        val settings = GameSettings(minPlayers = 2, allowSelfVote = true)
        val lobby = GameFixtures.lobby(players, settings)
        val withCards = engine.perform(lobby, GameCommand.SetCardPool(alice, GameFixtures.pool()))
        val running = engine.perform(withCards, GameCommand.Start(alice))
        val alone = engine.perform(running, GameCommand.PlayCards(alice, listOf(CardId("p1"))))
        val closed = engine.perform(alone, GameCommand.CloseSubmissions)

        assertTrue(alice in RoundProgress.voters(closed))
    }

    @Test
    fun `the czar is the only voter in czar mode`() {
        val running = startedGame(SelectionMode.CZAR)
        val answered = listOf(bob to "p11", carl to "p21").fold(running) { state, (player, card) ->
            engine.perform(state, GameCommand.PlayCards(player, listOf(CardId(card))))
        }

        assertEquals(listOf(alice), RoundProgress.voters(answered))
    }

    @Test
    fun `a step with no answer at all is already complete`() {
        val running = startedGame(SelectionMode.VOTE)
        val closed = engine.perform(running, GameCommand.CloseSubmissions)

        assertTrue(RoundProgress.selectionComplete(closed))
    }

    private fun startedGame(mode: SelectionMode): GameState {
        val settings = GameSettings(minPlayers = 2, selectionMode = mode)
        val lobby = GameFixtures.lobby(players, settings)
        val withCards = engine.perform(lobby, GameCommand.SetCardPool(alice, GameFixtures.pool()))
        return engine.perform(withCards, GameCommand.Start(alice))
    }

    private fun everybodyAnswers(running: GameState): GameState =
        listOf(alice to "p1", bob to "p11", carl to "p21").fold(running) { state, (player, card) ->
            engine.perform(state, GameCommand.PlayCards(player, listOf(CardId(card))))
        }
}
