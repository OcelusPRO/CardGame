package fr.ftnl.cardgame.api.dto

import kotlinx.serialization.Serializable

/** A pack offered in the lobby, with how many cards it brings. */
@Serializable
data class CardPackView(
    val id: String,
    val name: String,
    val description: String,
    val situationCount: Int,
    val punchlineCount: Int,
)
