package fr.ftnl.cardgame.api.dto

import kotlinx.serialization.Serializable

/** How much fuel the game has left, shown in the lobby and in the header. */
@Serializable
data class DeckSummary(
    val situationsLeft: Int,
    val punchlinesLeft: Int,
)
