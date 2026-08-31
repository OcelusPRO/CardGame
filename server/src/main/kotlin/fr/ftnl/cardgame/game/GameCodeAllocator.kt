package fr.ftnl.cardgame.game

import fr.ftnl.cardgame.domain.game.GameCode
import fr.ftnl.cardgame.domain.game.GameCodeGenerator
import fr.ftnl.cardgame.session.GameSessionStore

/** Draws game codes until it finds one no live game is using. */
class GameCodeAllocator(
    private val generator: GameCodeGenerator,
    private val store: GameSessionStore,
) {

    suspend fun allocate(): GameCode {
        repeat(MAX_ATTEMPTS) {
            val candidate = generator.generate()
            if (!store.exists(candidate)) return candidate
        }
        error("Could not allocate a free game code after $MAX_ATTEMPTS attempts")
    }

    private companion object {
        const val MAX_ATTEMPTS = 20
    }
}
