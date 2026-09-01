package fr.ftnl.cardgame.game

import fr.ftnl.cardgame.domain.game.GameCode
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.player.PlayerId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Time based, on purpose: the reaper is a wall-clock timer. The windows are tiny but
 * generously spaced so a slow machine still tells the two outcomes apart.
 */
class IdleGameReaperTest {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val forgotten = CopyOnWriteArrayList<GameCode>()

    private fun reaper(idleMillis: Long) = IdleGameReaper(scope, idleMillis) { forgotten += it }

    private fun game(code: String): GameState =
        GameState(code = GameCode.of(code), hostId = PlayerId("host"))

    @AfterTest
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `a table nobody touches is dropped once the idle window passes`() = runBlocking {
        reaper(idleMillis = 60).onGameCreated(game("AAAAA"))

        delay(300)

        assertEquals(listOf(GameCode.of("AAAAA")), forgotten.toList())
    }

    @Test
    fun `every change pushes the deadline back`() = runBlocking {
        val reaper = reaper(idleMillis = 150)
        val state = game("BBBBB")
        reaper.onGameCreated(state)

        repeat(4) {
            delay(80)
            reaper.onGameChanged(state, emptyList())
        }
        assertTrue(forgotten.isEmpty(), "the game was still being changed")

        delay(400)
        assertEquals(listOf(GameCode.of("BBBBB")), forgotten.toList())
    }

    @Test
    fun `a game forgotten by other means cancels its pending sweep`() = runBlocking {
        val reaper = reaper(idleMillis = 60)
        reaper.onGameCreated(game("CCCCC"))

        reaper.onGameForgotten(GameCode.of("CCCCC"))
        delay(300)

        assertTrue(forgotten.isEmpty())
    }
}
