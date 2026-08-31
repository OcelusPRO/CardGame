package fr.ftnl.cardgame.game

import fr.ftnl.cardgame.domain.engine.CommandResult
import fr.ftnl.cardgame.domain.engine.GameCommand
import fr.ftnl.cardgame.domain.engine.GameEngine
import fr.ftnl.cardgame.domain.game.GameCode
import fr.ftnl.cardgame.domain.game.GameSettings
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.player.Player
import fr.ftnl.cardgame.session.GameSessionStore
import java.util.concurrent.CopyOnWriteArrayList

/**
 * The single door between the transport layer and the game rules. It loads a snapshot,
 * runs one command against it under a per game lock, stores the result and tells the
 * listeners what happened.
 */
class GameService(
    private val store: GameSessionStore,
    private val engine: GameEngine,
    private val locks: GameLocks,
    private val codes: GameCodeAllocator,
    private val factory: GameFactory,
) {
    private val listeners = CopyOnWriteArrayList<GameListener>()

    fun addListener(listener: GameListener) {
        listeners += listener
    }

    suspend fun create(host: Player, settings: GameSettings): GameState {
        val state = factory.create(codes.allocate(), host, settings)
        store.save(state)
        listeners.forEach { it.onGameCreated(state) }
        return state
    }

    suspend fun find(code: GameCode): GameState? = store.find(code)

    suspend fun dispatch(code: GameCode, command: GameCommand): DispatchResult =
        runCommand(code, command).also { publish(it) }

    /** Drops a game from the store, used once every player has left the table. */
    suspend fun forget(code: GameCode) {
        store.delete(code)
        locks.release(code)
    }

    private suspend fun runCommand(code: GameCode, command: GameCommand): DispatchResult =
        locks.withLock(code) {
            val state = store.find(code) ?: return@withLock DispatchResult.GameNotFound
            when (val result = engine.execute(state, command)) {
                is CommandResult.Rejected -> DispatchResult.Refused(result.error)
                is CommandResult.Accepted -> save(result)
            }
        }

    private suspend fun save(result: CommandResult.Accepted): DispatchResult {
        store.save(result.state)
        return DispatchResult.Updated(result.state, result.events)
    }

    /** Listeners run outside the game lock so a slow broadcast never blocks a player. */
    private suspend fun publish(result: DispatchResult) {
        if (result !is DispatchResult.Updated) return
        listeners.forEach { it.onGameChanged(result.state, result.events) }
    }

    /** True once nobody is online any more, which is when the session can be dropped. */
    fun isAbandoned(state: GameState): Boolean = state.connectedPlayers.isEmpty()
}
