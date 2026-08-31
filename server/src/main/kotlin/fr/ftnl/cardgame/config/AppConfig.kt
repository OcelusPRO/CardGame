package fr.ftnl.cardgame.config

/** Everything the application reads from its environment, resolved once at boot. */
data class AppConfig(
    val database: DatabaseConfig,
    val redis: RedisConfig,
    val discord: DiscordConfig,
    val admin: AdminConfig,
    val session: SessionConfig,
    val seed: SeedConfig,
)
