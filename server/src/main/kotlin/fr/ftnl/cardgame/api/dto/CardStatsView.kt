package fr.ftnl.cardgame.api.dto

import kotlinx.serialization.Serializable

/**
 * The play record of a single punchline card, shown when an administrator looks one up:
 * how often it reached a hand, was played and voted for, plus the situation it did best
 * against.
 */
@Serializable
data class CardStatsView(
    val cardId: String,
    val text: String,
    val deals: Long,
    val plays: Long,
    val votes: Long,
    val wins: Long,
    val bestSituation: BestSituationView? = null,
) {
    @Serializable
    data class BestSituationView(
        val situationId: String,
        val text: String,
        val plays: Long,
        val votes: Long,
        val wins: Long,
    )
}
