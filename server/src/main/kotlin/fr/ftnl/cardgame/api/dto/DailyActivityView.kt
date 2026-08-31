package fr.ftnl.cardgame.api.dto

import kotlinx.serialization.Serializable

/** One point of the activity chart. */
@Serializable
data class DailyActivityView(
    val day: String,
    val gamesCreated: Long,
    val roundsPlayed: Long,
    val answersPlayed: Long,
)
