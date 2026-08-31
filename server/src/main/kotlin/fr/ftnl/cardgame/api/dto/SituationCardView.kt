package fr.ftnl.cardgame.api.dto

import kotlinx.serialization.Serializable

/** The situation of the round, with the number of holes the client has to render. */
@Serializable
data class SituationCardView(
    val id: String,
    val text: String,
    val blankCount: Int,
    val custom: Boolean,
)
