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
    private val dispatch: suspend (GameCode, GameCommand) -> Unit,
) : GameListener {

    private val jobs = ConcurrentHashMap<String, Job>()

    override suspend fun onGameChanged(state: GameState, events: List<GameEvent>) = reschedule(state)

    private fun reschedule(state: GameState) {
        jobs.remove(state.code.value)?.cancel()
        val command = commandFor(state.phase) ?: return
        val deadline = state.phaseDeadlineMillis ?: return
        jobs[state.code.value] = scope.launch {
            delay((deadline - clock.nowMillis()).coerceAtLeast(0))
            dispatch(state.code, command)
        }
    }

    private fun commandFor(phase: GamePhase): GameCommand? = when (phase) {
        GamePhase.SUBMITTING -> GameCommand.CloseSubmissions
        GamePhase.SELECTING -> GameCommand.CloseSelection
        GamePhase.ROUND_RESULT -> GameCommand.NextRound(by = null)
        GamePhase.LOBBY, GamePhase.FINISHED -> null
    }
}
