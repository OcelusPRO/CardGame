package fr.ftnl.cardgame.api.dto

import kotlinx.serialization.Serializable

/** Payload to add an account to the adult-pack allowlist. */
@Serializable
data class AdultAccessInput(
    /** `DISCORD` or `TWITCH`; an id only means something next to its provider. */
    val provider: String = "DISCORD",
    val accountId: String,
    /** A free note so the list stays readable, e.g. the person's name. */
    val label: String = "",
)
