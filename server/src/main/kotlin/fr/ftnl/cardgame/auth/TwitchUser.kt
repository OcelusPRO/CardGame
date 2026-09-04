package fr.ftnl.cardgame.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The slice of the Twitch profile we actually use: a name, a picture, and above all the
 * [login], which is both the channel address and the room its chat lives in.
 */
@Serializable
data class TwitchUser(
    val id: String,
    val login: String,
    @SerialName("display_name") val displayName: String = login,
    @SerialName("profile_image_url") val profileImageUrl: String? = null,
)

/** Helix answers a list, even when it is the single account that signed in. */
@Serializable
data class TwitchUsers(val data: List<TwitchUser> = emptyList())
