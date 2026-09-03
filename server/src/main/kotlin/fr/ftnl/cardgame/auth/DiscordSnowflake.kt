package fr.ftnl.cardgame.auth

/**
 * A Discord id is a "snowflake": its top 42 bits are the creation time, in milliseconds
 * since the first second of 2015. That means the account's age is already in the id we
 * hold from sign in — no extra API call, no question asked.
 */
object DiscordSnowflake {

    private const val DISCORD_EPOCH_MILLIS = 1_420_070_400_000L

    /** Epoch millis the account was created, or null when [id] is not a snowflake. */
    fun createdAtMillis(id: String): Long? {
        val snowflake = id.toLongOrNull()?.takeIf { it > 0 } ?: return null
        return (snowflake ushr 22) + DISCORD_EPOCH_MILLIS
    }
}
