package fr.ftnl.cardgame.auth

import kotlinx.serialization.Serializable

/**
 * The identity carried by the browser cookie. It survives a refresh, which is what lets
 * a player come back to their seat, and it is the only thing the WebSocket trusts.
 *
 * Both sign ins are optional and independent: a player may carry one, the other, or both.
 * [twitchLogin] is the channel name, and the only field the chat reader ever needs.
 */
@Serializable
data class PlayerSession(
    val playerId: String,
    val discordId: String? = null,
    val discordUsername: String? = null,
    val discordAvatarUrl: String? = null,
    val twitchId: String? = null,
    val twitchLogin: String? = null,
    val twitchUsername: String? = null,
    val twitchAvatarUrl: String? = null,
)
