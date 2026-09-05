package fr.ftnl.cardgame.config

import io.ktor.server.config.ApplicationConfig

/** Turns the raw Ktor configuration tree into the typed settings used by the services. */
object AppConfigLoader {

    fun load(config: ApplicationConfig): AppConfig = AppConfig(
        database = database(config),
        redis = redis(config),
        discord = discord(config),
        twitch = twitch(config),
        admin = AdminConfig(
            discordIds = config.list("app.admin.discordIds"),
            twitchIds = config.list("app.admin.twitchIds"),
        ),
        adultAccess = AdultAccessConfig(
            minAccountAgeDays = config.number("app.adultAccess.minAccountAgeDays", default = 1095),
        ),
        session = SessionConfig(config.text("app.session.signKey")),
        seed = SeedConfig(config.flag("app.seed.enabled")),
    )

    private fun database(config: ApplicationConfig) = DatabaseConfig(
        url = config.text("app.database.url"),
        user = config.text("app.database.user"),
        password = config.text("app.database.password"),
        poolSize = config.number("app.database.poolSize", default = 8),
        driver = config.text("app.database.driver"),
    )

    private fun redis(config: ApplicationConfig) = RedisConfig(
        url = config.text("app.redis.url"),
        sessionTtlMinutes = config.number("app.redis.sessionTtlMinutes", default = 180).toLong(),
        enabled = config.flag("app.redis.enabled"),
    )

    private fun discord(config: ApplicationConfig) = DiscordConfig(
        clientId = config.text("app.discord.clientId"),
        clientSecret = config.text("app.discord.clientSecret"),
        redirectUrl = config.text("app.discord.redirectUrl"),
        botToken = config.text("app.discord.botToken"),
    )

    private fun twitch(config: ApplicationConfig) = TwitchConfig(
        clientId = config.text("app.twitch.clientId"),
        clientSecret = config.text("app.twitch.clientSecret"),
        redirectUrl = config.text("app.twitch.redirectUrl"),
    )

    private fun ApplicationConfig.text(path: String): String =
        propertyOrNull(path)?.getString().orEmpty()

    private fun ApplicationConfig.flag(path: String): Boolean = text(path).toBoolean()

    private fun ApplicationConfig.number(path: String, default: Int): Int =
        text(path).toIntOrNull() ?: default

    /** Reads a comma separated list, tolerating spaces and a trailing separator. */
    private fun ApplicationConfig.list(path: String): Set<String> =
        text(path).split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
}
