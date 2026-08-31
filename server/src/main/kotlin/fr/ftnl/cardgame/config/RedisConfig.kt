package fr.ftnl.cardgame.config

/**
 * Redis holds the live games only. [sessionTtlMinutes] is refreshed on every write, so an
 * abandoned game disappears on its own without any cleanup job.
 */
data class RedisConfig(
    val url: String,
    val sessionTtlMinutes: Long,
    val enabled: Boolean,
)
