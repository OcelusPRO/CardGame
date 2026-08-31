package fr.ftnl.cardgame.domain.rules

import fr.ftnl.cardgame.domain.deck.DrawPile
import fr.ftnl.cardgame.domain.game.GameSettings
import fr.ftnl.cardgame.domain.game.Round
import fr.ftnl.cardgame.domain.game.Scoreboard
import fr.ftnl.cardgame.domain.player.PlayerId
import fr.ftnl.cardgame.domain.support.GameFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameEndConditionTest {

    private val alice = PlayerId("alice")
    private val bob = PlayerId("bob")

    private fun state(
        rounds: Int = 5,
        points: Map<PlayerId, Int> = emptyMap(),
        roundNumber: Int = 1,
        situationsLeft: Int = 3,
    ) = GameFixtures.lobby(
        GameFixtures.players("alice", "bob"),
        GameSettings(minPlayers = 2, rounds = rounds),
    ).copy(
        scoreboard = Scoreboard(points),
        round = Round(roundNumber, GameFixtures.situation("s1")),
        situations = DrawPile((1..situationsLeft).map { GameFixtures.situation("left$it") }),
    )

    @Test
    fun `keeps going while planned rounds are left`() {
        assertFalse(GameEndCondition.isReached(state(rounds = 5, roundNumber = 3)))
    }

    @Test
    fun `stops after the last planned round`() {
        assertTrue(GameEndCondition.isReached(state(rounds = 5, roundNumber = 5)))
    }

    @Test
    fun `a huge score never ends the game early`() {
        assertFalse(GameEndCondition.isReached(state(rounds = 5, roundNumber = 2, points = mapOf(alice to 999))))
    }

    @Test
    fun `stops when the situation deck is exhausted before the planned rounds`() {
        assertTrue(GameEndCondition.isReached(state(rounds = 20, roundNumber = 2, situationsLeft = 0)))
    }

    @Test
    fun `the winner is simply the best score`() {
        val board = mapOf(alice to 12, bob to 4)

        assertEquals(listOf(alice), GameEndCondition.winners(state(points = board)))
    }

    @Test
    fun `a tie leaves several winners`() {
        val board = mapOf(alice to 7, bob to 7)

        assertEquals(setOf(alice, bob), GameEndCondition.winners(state(points = board)).toSet())
    }
}
