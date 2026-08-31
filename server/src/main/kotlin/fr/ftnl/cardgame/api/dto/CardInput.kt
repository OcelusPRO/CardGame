package fr.ftnl.cardgame.api.dto

import kotlinx.serialization.Serializable

/** Payload used to create or edit an official card from the administration. */
@Serializable
data class CardInput(
    val id: String? = null,
    val packId: String,
    val text: String,
    val enabled: Boolean = true,
)
