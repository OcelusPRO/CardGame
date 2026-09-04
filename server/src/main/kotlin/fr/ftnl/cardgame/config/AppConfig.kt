package fr.ftnl.cardgame.config

/** Everything the application reads from its environment, resolved once at boot. */
data class AppConfig(
    val database: DatabaseConfig,
    val redis: RedisConfig,
    val discord: DiscordConfig,
    val twitch: TwitchConfig,
    val admin: AdminConfig,
    val adultAccess: AdultAccessConfig,
    val session: SessionConfig,
    val seed: SeedConfig,
)
