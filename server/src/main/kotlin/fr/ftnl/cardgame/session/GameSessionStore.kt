package fr.ftnl.cardgame.session

import fr.ftnl.cardgame.domain.game.GameCode
import fr.ftnl.cardgame.domain.game.GameState

/**
 * Where live games live. Sessions are short lived by design: they expire on their own
 * and nothing about a finished game is ever kept.
 */
interface GameSessionStore {
    suspend fun find(code: GameCode): GameState?
    suspend fun exists(code: GameCode): Boolean
    suspend fun save(state: GameState)
    suspend fun delete(code: GameCode)
}
