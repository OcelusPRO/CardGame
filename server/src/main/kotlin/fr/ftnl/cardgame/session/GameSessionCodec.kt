package fr.ftnl.cardgame.session

import fr.ftnl.cardgame.domain.game.GameState
import kotlinx.serialization.json.Json

/** Turns a game snapshot into the JSON blob stored in Redis, and back. */
class GameSessionCodec(private val json: Json = DEFAULT) {

    fun encode(state: GameState): String = json.encodeToString(GameState.serializer(), state)

    fun decode(payload: String): GameState = json.decodeFromString(GameState.serializer(), payload)

    /** Returns null instead of throwing when a snapshot predates a model change. */
    fun decodeOrNull(payload: String): GameState? = runCatching { decode(payload) }.getOrNull()

    private companion object {
        val DEFAULT = Json { ignoreUnknownKeys = true }
    }
}
