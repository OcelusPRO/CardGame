package fr.ftnl.cardgame.api.dto

import kotlinx.serialization.Serializable

/** One entry of the adult-pack allowlist, as listed in the administration. */
@Serializable
data class AdultAccessView(
    val provider: String,
    val accountId: String,
    val label: String,
    val addedAtMillis: Long,
)
