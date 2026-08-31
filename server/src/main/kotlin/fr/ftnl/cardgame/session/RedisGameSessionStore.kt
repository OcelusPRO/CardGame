package fr.ftnl.cardgame.session

import fr.ftnl.cardgame.domain.game.GameCode
import fr.ftnl.cardgame.domain.game.GameState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import redis.clients.jedis.JedisPooled

/**
 * Keeps live games in Redis. Every write refreshes the expiry, so a table nobody
 * came back to simply vanishes without any cleanup job.
 */
class RedisGameSessionStore(
    private val redis: JedisPooled,
    private val codec: GameSessionCodec,
    ttlMinutes: Long,
) : GameSessionStore {

    private val ttlSeconds = ttlMinutes * SECONDS_PER_MINUTE

    override suspend fun find(code: GameCode): GameState? = io {
        redis.get(key(code))?.let(codec::decodeOrNull)
    }

    override suspend fun exists(code: GameCode): Boolean = io { redis.exists(key(code)) }

    override suspend fun save(state: GameState): Unit = io {
        redis.setex(key(state.code), ttlSeconds, codec.encode(state))
    }

    override suspend fun delete(code: GameCode): Unit = io { redis.del(key(code)) }

    private fun key(code: GameCode) = "$KEY_PREFIX${code.value}"

    private suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { block() }

    private companion object {
        const val KEY_PREFIX = "cardgame:game:"
        const val SECONDS_PER_MINUTE = 60L
    }
}
