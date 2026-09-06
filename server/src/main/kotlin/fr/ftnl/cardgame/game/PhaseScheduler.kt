package fr.ftnl.cardgame.game

import fr.ftnl.cardgame.domain.engine.GameCommand
import fr.ftnl.cardgame.domain.engine.GameEvent
import fr.ftnl.cardgame.domain.game.GameClock
import fr.ftnl.cardgame.domain.game.GameCode
import fr.ftnl.cardgame.domain.game.GamePhase
import fr.ftnl.cardgame.domain.game.GameState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Turns the deadline carried by a snapshot into a real timer. Each game has at most one
 * pending job, cancelled and replaced every time the game moves on.
 */
class PhaseScheduler(
    private val scope: CoroutineScope,
    private val clock: GameClock,
    /**
     * Commands to run just before the phase closes, in order and under the same lock.
     * This is where state that was deliberately kept out of the snapshot — the running
     * Twitch chat tally — is banked, at the one moment it stops being a display.
     */
    private val beforeDeadline: suspend (GameState) -> List<GameCommand> = { emptyList() },
    private val dispatch: suspend (GameCode, GameCommand) -> Unit,
) : GameListener {

    private val jobs = ConcurrentHashMap<String, Job>()

    override suspend fun onGameChanged(state: GameState, events: List<GameEvent>) = reschedule(state)

    override suspend fun onGameForgotten(code: GameCode) {
        jobs.remove(code.value)?.cancel()
    }

    private fun reschedule(state: GameState) {
        jobs.remove(state.code.value)?.cancel()
        val command = commandFor(state.phase) ?: return
        val deadline = state.phaseDeadlineMillis ?: return
        jobs[state.code.value] = scope.launch {
            delay((deadline - clock.nowMillis()).coerceAtLeast(0))
            beforeDeadline(state).forEach { dispatch(state.code, it) }
            dispatch(state.code, command)
        }
    }

    private fun commandFor(phase: GamePhase): GameCommand? = when (phase) {
        GamePhase.SUBMITTING -> GameCommand.CloseSubmissions
        GamePhase.SELECTING -> GameCommand.CloseSelection
        GamePhase.ROUND_RESULT -> GameCommand.NextRound(by = null)
        GamePhase.LOBBY, GamePhase.FINISHED -> null
    }

    /** How many timers are pending; it should track the number of live games, not exceed it. */
    val size: Int get() = jobs.size
}
