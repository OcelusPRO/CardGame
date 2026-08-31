package fr.ftnl.cardgame.api.dto

import kotlinx.serialization.Serializable

/** The header of the administration dashboard: catalogue size and today activity. */
@Serializable
data class AdminOverview(
    val packs: Int,
    val situations: Long,
    val punchlines: Long,
    val live: LiveStatsView,
    val today: DailyActivityView,
)
