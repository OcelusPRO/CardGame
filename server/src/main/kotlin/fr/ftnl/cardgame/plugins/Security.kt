package fr.ftnl.cardgame.plugins

import fr.ftnl.cardgame.auth.AdminSession
import fr.ftnl.cardgame.auth.PlayerSession
import fr.ftnl.cardgame.config.AppConfig
import fr.ftnl.cardgame.config.DiscordConfig
import io.ktor.client.HttpClient
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.OAuthServerSettings
import io.ktor.server.auth.oauth
import io.ktor.server.sessions.SessionTransportTransformerMessageAuthentication
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie

const val DISCORD_PROVIDER = "discord"

private const val PLAYER_COOKIE = "cardgame_player"
private const val ADMIN_COOKIE = "cardgame_admin"
private const val COOKIE_MAX_AGE_SECONDS = 60L * 60 * 24 * 30

/**
 * Signed cookies carry the player identity and, for the few allowlisted accounts, the
 * administrator identity. Discord sign in is registered only when it is configured.
 */
fun Application.configureSecurity(config: AppConfig, httpClient: HttpClient) {
    val signKey = config.session.signKey.toByteArray()

    install(Sessions) {
        cookie<PlayerSession>(PLAYER_COOKIE) {
            cookie.path = "/"
            cookie.httpOnly = true
            cookie.maxAgeInSeconds = COOKIE_MAX_AGE_SECONDS
            cookie.extensions["SameSite"] = "Lax"
            transform(SessionTransportTransformerMessageAuthentication(signKey))
        }
        cookie<AdminSession>(ADMIN_COOKIE) {
            cookie.path = "/"
            cookie.httpOnly = true
            cookie.maxAgeInSeconds = COOKIE_MAX_AGE_SECONDS
            cookie.extensions["SameSite"] = "Lax"
            transform(SessionTransportTransformerMessageAuthentication(signKey))
        }
    }

    if (!config.discord.enabled) return
    install(Authentication) {
        oauth(DISCORD_PROVIDER) {
            urlProvider = { config.discord.redirectUrl }
            providerLookup = { discordSettings(config.discord) }
            client = httpClient
        }
    }
}

private fun discordSettings(config: DiscordConfig) = OAuthServerSettings.OAuth2ServerSettings(
    name = DISCORD_PROVIDER,
    authorizeUrl = "https://discord.com/api/oauth2/authorize",
    accessTokenUrl = "https://discord.com/api/oauth2/token",
    requestMethod = HttpMethod.Post,
    clientId = config.clientId,
    clientSecret = config.clientSecret,
    defaultScopes = listOf("identify"),
)
