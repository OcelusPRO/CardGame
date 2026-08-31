package fr.ftnl.cardgame.auth

import kotlinx.serialization.Serializable

/**
 * The identity carried by the browser cookie. It survives a refresh, which is what lets
 * a player come back to their seat, and it is the only thing the WebSocket trusts.
 */
@Serializable
data class PlayerSession(
    val playerId: String,
    val discordId: String? = null,
    val discordUsername: String? = null,
    val discordAvatarUrl: String? = null,
)
