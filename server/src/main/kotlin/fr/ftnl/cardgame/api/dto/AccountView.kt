package fr.ftnl.cardgame.api.dto

import kotlinx.serialization.Serializable

/** An account the administration looked up, so a bare id gets a face and a name. */
@Serializable
data class AccountView(
    val provider: String,
    val accountId: String,
    val name: String,
    /** The Twitch channel name; absent on Discord, which has no such handle here. */
    val login: String? = null,
    val avatarUrl: String? = null,
)
