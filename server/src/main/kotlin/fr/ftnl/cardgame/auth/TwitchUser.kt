package fr.ftnl.cardgame.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * The slice of the Twitch profile we actually use: a name, a picture, the [login] — which
 * is both the channel address and the room its chat lives in — and the day the account
 * was opened, which is what the adult-pack age rule reads.
 *
 * A Twitch id carries no date of its own, unlike a Discord snowflake, so [createdAt] is
 * the only way to tell a ten year old account from one opened this morning.
 */
@Serializable
data class TwitchUser(
    val id: String,
    val login: String,
    @SerialName("display_name") val displayName: String = login,
    @SerialName("profile_image_url") val profileImageUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
) {
    /** Epoch millis the account was created, or null when Twitch sent nothing usable. */
    val createdAtMillis: Long?
        get() = createdAt?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }

    /** The account as the rest of the application knows it, provider included. */
    fun account(): Account = Account(
        provider = AccountProvider.TWITCH,
        id = id,
        displayName = displayName,
        createdAtMillis = createdAtMillis,
    )
}

/** Helix answers a list, even when it is the single account that signed in. */
@Serializable
data class TwitchUsers(val data: List<TwitchUser> = emptyList())
