package fr.ftnl.cardgame.api.dto

import kotlinx.serialization.Serializable

/** Avatar chosen in the customiser, sent when creating or joining a game. */
@Serializable
data class AvatarInput(
    val topStyleId: String,
    val topColor: String,
    val bottomStyleId: String,
    val bottomColor: String,
)
