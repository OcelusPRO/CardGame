package fr.ftnl.cardgame.auth

import kotlinx.serialization.Serializable

/** Set only for the Discord accounts listed in `ADMIN_DISCORD_IDS`. */
@Serializable
data class AdminSession(
    val discordId: String,
    val username: String,
)
