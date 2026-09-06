package fr.ftnl.cardgame.auth

import fr.ftnl.cardgame.config.TwitchConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * The consent a host gave to have channel point rewards created on their channel.
 *
 * It is kept here, on the server, and never in the cookie: a browser holds an identity,
 * not a right to act on somebody's channel. One entry per Twitch account, so a host who
 * signs in from a second browser is already authorised, and a host who signs out of the
 * game is not — the two are simply different questions.
 *
 * Nothing is written down. A restart loses the consents, and the hosts who want the mode
 * grant it again in one click; that is a far better trade than a table of live Twitch
 * tokens sitting in a database.
 */
class TwitchHostTokens(
    private val http: HttpClient,
    private val config: TwitchConfig,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val mutex = Mutex()
    private val granted = ConcurrentHashMap<String, Grant>()

    /** What the consent callback came back with, for [twitchId] and nobody else. */
    fun remember(twitchId: String, accessToken: String, refreshToken: String?, expiresInSeconds: Long) {
        granted[twitchId] = Grant(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAtMillis = expiryOf(expiresInSeconds),
        )
    }

    /** Whether this account has opened its channel points to the game. */
    fun holds(twitchId: String?): Boolean = twitchId != null && granted.containsKey(twitchId)

    fun forget(twitchId: String) {
        granted.remove(twitchId)
    }

    val size: Int get() = granted.size

    /**
     * A token good for right now, renewed when it is about to lapse. Null once Twitch
     * stops renewing it — the host revoked the game, and the mode simply closes rather
     * than pretending it still works.
     */
    suspend fun token(twitchId: String): String? {
        granted[twitchId]?.takeIf { nowMillis() < it.expiresAtMillis }?.let { return it.accessToken }
        mutex.withLock {
            val grant = granted[twitchId] ?: return null
            if (nowMillis() < grant.expiresAtMillis) return grant.accessToken
            val refresh = grant.refreshToken ?: return dropped(twitchId)
            val renewed = refresh(refresh) ?: return dropped(twitchId)
            val next = Grant(
                accessToken = renewed.accessToken,
                // Twitch hands back a fresh refresh token; the old one stops working.
                refreshToken = renewed.refreshToken ?: refresh,
                expiresAtMillis = expiryOf(renewed.expiresIn),
            )
            granted[twitchId] = next
            return next.accessToken
        }
    }

    private fun dropped(twitchId: String): String? {
        granted.remove(twitchId)
        return null
    }

    private fun expiryOf(seconds: Long): Long =
        nowMillis() + (seconds - EARLY_SECONDS).coerceAtLeast(0) * 1000L

    private suspend fun refresh(refreshToken: String): RefreshedToken? = runCatching {
        val response: HttpResponse = http.submitForm(
            url = TOKEN_URL,
            formParameters = Parameters.build {
                append("client_id", config.clientId)
                append("client_secret", config.clientSecret)
                append("grant_type", "refresh_token")
                append("refresh_token", refreshToken)
            },
        )
        if (response.status.isSuccess()) response.body<RefreshedToken>() else null
    }.onFailure { log.warn("A Twitch host token could not be renewed", it) }.getOrNull()

    private data class Grant(
        val accessToken: String,
        val refreshToken: String?,
        val expiresAtMillis: Long,
    )

    @Serializable
    private data class RefreshedToken(
        @SerialName("access_token") val accessToken: String,
        @SerialName("refresh_token") val refreshToken: String? = null,
        @SerialName("expires_in") val expiresIn: Long = 3_600,
    )

    private companion object {
        const val TOKEN_URL = "https://id.twitch.tv/oauth2/token"

        /** Renewed a minute early, so a Helix call never rides an expiring token. */
        const val EARLY_SECONDS = 60
    }
}
