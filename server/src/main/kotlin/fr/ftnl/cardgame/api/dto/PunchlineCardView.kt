package fr.ftnl.cardgame.api.dto

import kotlinx.serialization.Serializable

/** A punchline card of the viewer hand. [blankCount] is the number of holes to fill in. */
@Serializable
data class PunchlineCardView(
    val id: String,
    val text: String,
    val custom: Boolean,
    val blankCount: Int = 0,
)
