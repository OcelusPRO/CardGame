package fr.ftnl.cardgame.game

import fr.ftnl.cardgame.domain.game.GameCode
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Serialises the commands of a single game. Two players clicking at the same instant
 * must not both read the same snapshot and overwrite each other.
 */
class GameLocks {

    private val mutexes = ConcurrentHashMap<String, Mutex>()

    suspend fun <T> withLock(code: GameCode, block: suspend () -> T): T =
        mutexes.computeIfAbsent(code.value) { Mutex() }.withLock { block() }

    /** Frees the mutex of a game that no longer exists. */
    fun release(code: GameCode) {
        mutexes.remove(code.value)
    }
}
