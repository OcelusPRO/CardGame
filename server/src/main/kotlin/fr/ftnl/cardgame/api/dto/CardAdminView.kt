package fr.ftnl.cardgame.api.dto

import kotlinx.serialization.Serializable

/** An official card as listed in the administration table. */
@Serializable
data class CardAdminView(
    val id: String,
    val packId: String,
    val text: String,
    val enabled: Boolean,
    val blankCount: Int? = null,
)
