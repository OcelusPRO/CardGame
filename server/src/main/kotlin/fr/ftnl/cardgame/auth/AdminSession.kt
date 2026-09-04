package fr.ftnl.cardgame.auth

import kotlinx.serialization.Serializable

/** Set only for the accounts listed in `ADMIN_DISCORD_IDS` or `ADMIN_TWITCH_IDS`. */
@Serializable
data class AdminSession(
    val provider: String,
    val accountId: String,
    val username: String,
)
