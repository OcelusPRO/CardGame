package fr.ftnl.cardgame.api.dto

import kotlinx.serialization.Serializable

/** Uniform error body: the client owns the translated wording behind [code]. */
@Serializable
data class ErrorResponse(
    val code: String,
    val detail: String? = null,
)
