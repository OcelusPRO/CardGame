package fr.ftnl.cardgame.support

import fr.ftnl.cardgame.config.AdminConfig
import fr.ftnl.cardgame.config.AppConfig
import fr.ftnl.cardgame.config.DatabaseConfig
import fr.ftnl.cardgame.config.DiscordConfig
import fr.ftnl.cardgame.config.RedisConfig
import fr.ftnl.cardgame.config.SeedConfig
import fr.ftnl.cardgame.config.SessionConfig

/** Configuration used by the tests: no Redis, no Discord, one allowlisted administrator. */
object TestConfig {

    const val ADMIN_DISCORD_ID = "admin-42"

    fun create(): AppConfig = AppConfig(
        database = DatabaseConfig("", "", "", 1, "org.postgresql.Driver"),
        redis = RedisConfig(url = "", sessionTtlMinutes = 60, enabled = false),
        discord = DiscordConfig(clientId = "", clientSecret = "", redirectUrl = ""),
        admin = AdminConfig(setOf(ADMIN_DISCORD_ID)),
        session = SessionConfig("test-sign-key-please-change-0123456789"),
        seed = SeedConfig(enabled = false),
    )
}
