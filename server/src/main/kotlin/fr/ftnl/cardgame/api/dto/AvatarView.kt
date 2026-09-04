package fr.ftnl.cardgame.api.dto

import kotlinx.serialization.Serializable

/** The two halves of an avatar, plus the profile picture drawn over the top one. */
@Serializable
data class AvatarView(
    val top: AvatarPartView,
    val bottom: AvatarPartView,
    val pictureUrl: String? = null,
)
