package fr.ftnl.cardgame.twitch

import fr.ftnl.cardgame.auth.TwitchClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import fr.ftnl.cardgame.config.TwitchConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * The profile pictures of the viewers who voted.
 *
 * A chat line carries an account id and a name, never a picture, so the faces come from
 * Helix — read with an application token, since no viewer ever signed in here. Only the
 * handful of faces the table actually shows is ever looked up, and every answer is
 * remembered so a regular of the chat is asked for once.
 */
class TwitchViewers(
    private val client: TwitchClient,
    private val tokens: TwitchAppTokens,
) : ViewerPictures {

    private val log = LoggerFactory.getLogger(javaClass)

    /** Id to picture; an empty string marks "asked, and Twitch had nothing to show". */
    private val known = ConcurrentHashMap<String, String>()

    override suspend fun of(ids: Collection<String>): Map<String, String> {
        val wanted = ids.filter { it.isNotBlank() }.distinct()
        val unknown = wanted.filterNot(known::containsKey)
        if (unknown.isNotEmpty()) fetch(unknown)
        return wanted.mapNotNull { id -> known[id]?.takeIf { it.isNotEmpty() }?.let { id to it } }.toMap()
    }

    private suspend fun fetch(ids: List<String>) {
        val token = tokens.token() ?: return
        // Helix takes a hundred ids at a time, which is far more than a table ever shows.
        ids.chunked(HELIX_BATCH).forEach { batch ->
            val found = runCatching { client.viewers(token, batch) }
                .onFailure { log.warn("Twitch profile pictures could not be read", it) }
                .getOrNull() ?: return
            forget()
            found.forEach { known[it.id] = it.profileImageUrl.orEmpty() }
            // Ids Twitch did not answer for are marked too, so they are asked for once.
            batch.forEach { known.putIfAbsent(it, "") }
        }
    }

    /** A long lived server should not remember every viewer it ever saw. */
    private fun forget() {
        if (known.size > MAX_REMEMBERED) known.clear()
    }

    private companion object {
        const val HELIX_BATCH = 100
        const val MAX_REMEMBERED = 5_000
    }
}

/**
 * The application token used to read public Twitch profiles. It belongs to the game, not
 * to a player: nobody is asked for anything, and it is renewed a little before it lapses.
 */
class TwitchAppTokens(
    private val http: HttpClient,
    private val config: TwitchConfig,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val mutex = Mutex()
    private var token: String? = null
    private var expiresAtMillis = 0L

    suspend fun token(): String? {
        if (!config.enabled) return null
        mutex.withLock {
            token?.takeIf { nowMillis() < expiresAtMillis }?.let { return it }
            val granted = request() ?: return null
            token = granted.accessToken
            expiresAtMillis = nowMillis() + (granted.expiresIn - EARLY_SECONDS).coerceAtLeast(0) * 1000L
            return token
        }
    }

    private suspend fun request(): AppToken? = runCatching {
        val response: HttpResponse = http.submitForm(
            url = TOKEN_URL,
            formParameters = Parameters.build {
                append("client_id", config.clientId)
                append("client_secret", config.clientSecret)
                append("grant_type", "client_credentials")
            },
        )
        if (response.status.isSuccess()) response.body<AppToken>() else null
    }.onFailure { log.warn("Twitch application token could not be obtained", it) }.getOrNull()

    @Serializable
    private data class AppToken(
        @SerialName("access_token") val accessToken: String,
        @SerialName("expires_in") val expiresIn: Long = 3_600,
    )

    private companion object {
        const val TOKEN_URL = "https://id.twitch.tv/oauth2/token"

        /** Renewed a minute early, so a request never rides an expiring token. */
        const val EARLY_SECONDS = 60
    }
}
