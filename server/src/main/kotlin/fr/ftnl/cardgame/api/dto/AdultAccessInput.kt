package fr.ftnl.cardgame.api.dto

import kotlinx.serialization.Serializable

/** Payload to add a Discord account to the adult-pack allowlist. */
@Serializable
data class AdultAccessInput(
    val discordId: String,
    /** A free note so the list stays readable, e.g. the person's name. */
    val label: String = "",
)
