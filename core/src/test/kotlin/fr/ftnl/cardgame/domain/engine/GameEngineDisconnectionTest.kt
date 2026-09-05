package fr.ftnl.cardgame.domain.engine

import fr.ftnl.cardgame.domain.card.CardId
import fr.ftnl.cardgame.domain.game.GamePhase
import fr.ftnl.cardgame.domain.game.Round
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.game.Scoreboard
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

    @Test
    fun `a drop in the lobby only marks the player offline, the seat is kept for now`() {
        val lobby = GameFixtures.lobby(players)

        val state = engine.perform(lobby, GameCommand.SetConnected(carl, connected = false))

        assertEquals(3, state.players.size)
        assertFalse(state.players.single { it.id == carl }.connected)
    }

    @Test
    fun `the grace delay frees the seat of a lobby player who did not come back`() {
        val gone = engine.perform(
            GameFixtures.lobby(players),
            GameCommand.SetConnected(carl, connected = false),
        )

        val state = engine.perform(gone, GameCommand.DropIfAway(carl))

        assertEquals(listOf(alice, bob), state.players.map { it.id })
    }

    @Test
    fun `the grace delay leaves alone a player who reconnected in time`() {
        val flap = listOf(false, true).fold(GameFixtures.lobby(players)) { state, connected ->
            engine.perform(state, GameCommand.SetConnected(carl, connected = connected))
        }

        val state = engine.perform(flap, GameCommand.DropIfAway(carl))

        assertEquals(3, state.players.size)
        assertTrue(state.players.single { it.id == carl }.connected)
    }

    @Test
    fun `a host who drops hands the crown to the next player in`() {
        val lobby = GameFixtures.lobby(players)

        val state = engine.perform(lobby, GameCommand.SetConnected(alice, connected = false))

        assertEquals(bob, state.hostId)
        assertEquals(3, state.players.size)
        assertFalse(state.players.single { it.id == alice }.connected)
    }

    @Test
    fun `the crown skips a player who is also offline`() {
        val lobby = engine.perform(
            GameFixtures.lobby(players),
            GameCommand.SetConnected(bob, connected = false),
        )

        val state = engine.perform(lobby, GameCommand.SetConnected(alice, connected = false))

        assertEquals(carl, state.hostId)
    }

    @Test
    fun `the grace delay never unseats a player while the match is running`() {
        val gone = engine.perform(running, GameCommand.SetConnected(carl, connected = false))

        val state = engine.perform(gone, GameCommand.DropIfAway(carl))

        assertEquals(3, state.players.size)
        assertFalse(state.players.single { it.id == carl }.connected)
    }

    @Test
    fun `the grace delay never unseats a player once the game is over either`() {
        val lastRound = GameFixtures.lobby(players).copy(
            phase = GamePhase.ROUND_RESULT,
            round = Round(number = running.settings.rounds, situation = GameFixtures.situation("s")),
        )
        val ended = engine.perform(lastRound, GameCommand.NextRound(by = null))
        val gone = engine.perform(ended, GameCommand.SetConnected(carl, connected = false))

        val state = engine.perform(gone, GameCommand.DropIfAway(carl))

        assertEquals(3, state.players.size)
        assertFalse(state.players.single { it.id == carl }.connected)
    }

    @Test
    fun `a host who drops mid-game keeps the crown`() {
        // A page reload is exactly this: a drop, right in the middle of a round, where
        // handing the table to somebody else would only strand whoever is mid-action.
        val state = engine.perform(running, GameCommand.SetConnected(alice, connected = false))

        assertEquals(alice, state.hostId)
        assertFalse(state.players.single { it.id == alice }.connected)
    }

    @Test
    fun `reconnecting mid-game never had to fight over the crown in the first place`() {
        val gone = engine.perform(running, GameCommand.SetConnected(alice, connected = false))

        val back = engine.perform(gone, GameCommand.SetConnected(alice, connected = true))

        assertEquals(alice, back.hostId)
    }

    @Test
    fun `finishing the game keeps every player and their score, connected or not`() {
        val lastRound = GameFixtures.lobby(players).copy(
            phase = GamePhase.ROUND_RESULT,
            players = players.map { if (it.id == carl) it.copy(connected = false) else it },
            round = Round(number = running.settings.rounds, situation = GameFixtures.situation("s")),
            scoreboard = Scoreboard(mapOf(alice to 5, bob to 3, carl to 7)),
        )

        val ended = engine.perform(lastRound, GameCommand.NextRound(alice))

        assertEquals(GamePhase.FINISHED, ended.phase)
        assertEquals(listOf(alice, bob, carl), ended.players.map { it.id })
        assertEquals(7, ended.scoreboard.points[carl])
        // The host stayed connected throughout, so the crown never had to move.
        assertEquals(alice, ended.hostId)
    }

    @Test
    fun `the crown only passes on at the very end, and only if the host never came back`() {
        val lastRound = GameFixtures.lobby(players).copy(
            phase = GamePhase.ROUND_RESULT,
            players = players.map { if (it.id == alice) it.copy(connected = false) else it },
            round = Round(number = running.settings.rounds, situation = GameFixtures.situation("s")),
            scoreboard = Scoreboard(mapOf(alice to 5, bob to 3, carl to 7)),
        )

        // Alice, the disconnected host, cannot be the one asking for the next round: this
        // is the scheduler moving the game along on its own, exactly as it would in play.
        val ended = engine.perform(lastRound, GameCommand.NextRound(by = null))

        assertEquals(GamePhase.FINISHED, ended.phase)
        assertEquals(bob, ended.hostId)
        assertEquals(listOf(alice, bob, carl), ended.players.map { it.id })
        assertEquals(5, ended.scoreboard.points[alice])
    }

    private fun startedGame(): GameState {
        val lobby = GameFixtures.lobby(players)
        val withCards = engine.perform(lobby, GameCommand.SetCardPool(alice, GameFixtures.pool()))
        return engine.perform(withCards, GameCommand.Start(alice))
    }
}
