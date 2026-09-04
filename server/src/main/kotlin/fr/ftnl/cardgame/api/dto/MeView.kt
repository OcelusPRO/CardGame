package fr.ftnl.cardgame.api.dto

import kotlinx.serialization.Serializable

/** Who the browser is, and what it is allowed to do. */
@Serializable
data class MeView(
    val playerId: String,
    val discordConnected: Boolean,
    val discordUsername: String? = null,
    val discordAvatarUrl: String? = null,
    val twitchConnected: Boolean = false,
    val twitchUsername: String? = null,
    val twitchAvatarUrl: String? = null,
    /** The channel name, which is what the table reads a chat from. */
    val twitchLogin: String? = null,
    val isAdmin: Boolean,
    val discordLoginAvailable: Boolean,
    val twitchLoginAvailable: Boolean = false,
)
