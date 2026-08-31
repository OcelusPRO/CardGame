package fr.ftnl.cardgame.api.dto

import kotlinx.serialization.Serializable

/** One half of an avatar as it travels to the browser. */
@Serializable
data class AvatarPartView(
    val styleId: String,
    val color: String,
)
