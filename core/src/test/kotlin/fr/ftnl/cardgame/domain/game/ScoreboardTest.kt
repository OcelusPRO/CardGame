package fr.ftnl.cardgame.domain.game

import fr.ftnl.cardgame.domain.player.PlayerId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScoreboardTest {

    private val alice = PlayerId("alice")
    private val bob = PlayerId("bob")

    @Test
    fun `unknown players are worth zero`() {
        assertEquals(0, Scoreboard().pointsOf(alice))
    }

    @Test
    fun `adds a round outcome to the running totals`() {
        val board = Scoreboard(mapOf(alice to 2)) + mapOf(alice to 3, bob to 1)

        assertEquals(5, board.pointsOf(alice))
        assertEquals(1, board.pointsOf(bob))
    }

    @Test
    fun `registering a player keeps an existing score`() {
        val board = Scoreboard(mapOf(alice to 7)).withPlayer(alice).withPlayer(bob)

        assertEquals(7, board.pointsOf(alice))
        assertEquals(0, board.pointsOf(bob))
    }

    @Test
    fun `leaders lists every player tied at the top`() {
        val board = Scoreboard(mapOf(alice to 4, bob to 4, PlayerId("carl") to 1))

        assertEquals(setOf(alice, bob), board.leaders.toSet())
    }

    @Test
    fun `nobody leads before the first point`() {
        assertTrue(Scoreboard(mapOf(alice to 0)).leaders.isEmpty())
    }
}
