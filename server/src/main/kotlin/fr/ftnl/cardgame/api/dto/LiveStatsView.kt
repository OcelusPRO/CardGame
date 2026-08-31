package fr.ftnl.cardgame.api.dto

import kotlinx.serialization.Serializable

/** The counters pushed a few times a minute to the administration dashboard. */
@Serializable
data class LiveStatsView(
    val activeGames: Int,
    val connectedPlayers: Int,
    val timestampMillis: Long,
)
