package fr.ftnl.cardgame.game

import fr.ftnl.cardgame.domain.engine.GameEvent
import fr.ftnl.cardgame.domain.game.GameCode
import fr.ftnl.cardgame.domain.game.GameState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Drops a table that has gone quiet. Every accepted command — a rule change, someone
 * joining, a round moving on — pushes the deadline back; if nothing happens for
 * [idleMillis] the game is forgotten, whoever is still connected or not.
 *
 * In memory, like [PhaseScheduler]: a restart loses the pending deadlines, and the store
 * TTL is the backstop for that.
 */
class IdleGameReaper(
    private val scope: CoroutineScope,
    private val idleMillis: Long,
    private val forget: suspend (GameCode) -> Unit,
) : GameListener {

    private val jobs = ConcurrentHashMap<String, Job>()

    override suspend fun onGameCreated(state: GameState) = arm(state.code)

    override suspend fun onGameChanged(state: GameState, events: List<GameEvent>) = arm(state.code)

    override suspend fun onGameForgotten(code: GameCode) {
        jobs.remove(code.value)?.cancel()
    }

    private fun arm(code: GameCode) {
        val sweep = scope.launch {
            delay(idleMillis)
            // Only reap if this very job is still the one on file: a later arm() may have
            // replaced it (outside the game lock, so two can race) and the game is alive.
            if (jobs[code.value] === coroutineContext[Job]) {
                jobs.remove(code.value)
                forget(code)
            }
        }
        jobs.put(code.value, sweep)?.cancel()
    }
}
