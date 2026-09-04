package fr.ftnl.cardgame.support

import fr.ftnl.cardgame.config.AdminConfig
import fr.ftnl.cardgame.config.AdultAccessConfig
import fr.ftnl.cardgame.config.AppConfig
import fr.ftnl.cardgame.config.DatabaseConfig
import fr.ftnl.cardgame.config.DiscordConfig
import fr.ftnl.cardgame.config.RedisConfig
import fr.ftnl.cardgame.config.SeedConfig
import fr.ftnl.cardgame.config.SessionConfig
import fr.ftnl.cardgame.config.TwitchConfig

/** Configuration used by the tests: no Redis, no sign in, one allowlisted administrator. */
object TestConfig {

    const val ADMIN_DISCORD_ID = "admin-42"
    const val ADMIN_TWITCH_ID = "777"

    fun create(): AppConfig = AppConfig(
        database = DatabaseConfig("", "", "", 1, "org.postgresql.Driver"),
        redis = RedisConfig(url = "", sessionTtlMinutes = 60, enabled = false),
        discord = DiscordConfig(clientId = "", clientSecret = "", redirectUrl = ""),
        twitch = TwitchConfig(clientId = "", clientSecret = "", redirectUrl = ""),
        admin = AdminConfig(discordIds = setOf(ADMIN_DISCORD_ID), twitchIds = setOf(ADMIN_TWITCH_ID)),
        // Off by default so tests exercise the allowlist explicitly; the account-age
        // heuristic has its own unit test.
        adultAccess = AdultAccessConfig(minAccountAgeDays = 0),
        session = SessionConfig("test-sign-key-please-change-0123456789"),
        seed = SeedConfig(enabled = false),
    )

    /** The same configuration, with the "an old account is an adult" rule switched on. */
    fun trustingAccountAge(minAccountAgeDays: Int = 1095): AppConfig =
        create().let { it.copy(adultAccess = AdultAccessConfig(minAccountAgeDays)) }
}
