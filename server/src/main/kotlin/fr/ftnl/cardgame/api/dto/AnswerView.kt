package fr.ftnl.cardgame.api.dto

import kotlinx.serialization.Serializable

/**
 * An answer on the table. [authorId] and [votes] stay null while the round is being
 * judged, so nobody can tell who wrote what before the reveal.
 */
@Serializable
data class AnswerView(
    val id: Int,
    val texts: List<String>,
    val filledText: String,
    val authorId: String? = null,
    val votes: Int? = null,
    val isMine: Boolean = false,
)
