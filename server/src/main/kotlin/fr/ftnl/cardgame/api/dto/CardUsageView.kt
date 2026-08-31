package fr.ftnl.cardgame.api.dto

import kotlinx.serialization.Serializable

/** A card leaderboard row, text included. */
@Serializable
data class CardUsageView(
    val cardId: String,
    val kind: String,
    val text: String,
    val deals: Long,
    val plays: Long,
    val votes: Long,
    val wins: Long,
)
