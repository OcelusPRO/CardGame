package fr.ftnl.cardgame.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The slice of the Discord profile we actually use: a name and a picture. */
@Serializable
data class DiscordUser(
    val id: String,
    val username: String,
    @SerialName("global_name") val globalName: String? = null,
    val avatar: String? = null,
) {
    val displayName: String get() = globalName ?: username

    /**
     * Null when the account kept the default Discord picture. No file extension on purpose:
     * the Discord CDN then serves whatever the avatar really is, animated ones included.
     */
    val avatarUrl: String?
        get() = avatar?.let { "https://cdn.discordapp.com/avatars/$id/$it?size=128" }

    /** The account as the rest of the application knows it, provider included. */
    fun account(): Account = Account(
        provider = AccountProvider.DISCORD,
        id = id,
        displayName = displayName,
        createdAtMillis = DiscordSnowflake.createdAtMillis(id),
    )
}
