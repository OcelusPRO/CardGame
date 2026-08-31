package fr.ftnl.cardgame.session

import fr.ftnl.cardgame.domain.game.GameCode
import fr.ftnl.cardgame.domain.game.GameState
import java.util.concurrent.ConcurrentHashMap

/** Store used when Redis is turned off, in tests and for a quick local run. */
class InMemoryGameSessionStore : GameSessionStore {

    private val games = ConcurrentHashMap<String, GameState>()

    override suspend fun find(code: GameCode): GameState? = games[code.value]

    override suspend fun exists(code: GameCode): Boolean = games.containsKey(code.value)

    override suspend fun save(state: GameState) {
        games[state.code.value] = state
    }

    override suspend fun delete(code: GameCode) {
        games.remove(code.value)
    }
}
