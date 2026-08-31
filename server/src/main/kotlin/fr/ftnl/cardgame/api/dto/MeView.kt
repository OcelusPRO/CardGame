package fr.ftnl.cardgame.api.dto

import kotlinx.serialization.Serializable

/** Who the browser is, and what it is allowed to do. */
@Serializable
data class MeView(
    val playerId: String,
    val discordConnected: Boolean,
    val discordUsername: String? = null,
    val discordAvatarUrl: String? = null,
    val isAdmin: Boolean,
    val discordLoginAvailable: Boolean,
)
