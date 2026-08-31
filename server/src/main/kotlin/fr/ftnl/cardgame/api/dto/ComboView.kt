package fr.ftnl.cardgame.api.dto

import kotlinx.serialization.Serializable

/**
 * A situation and punchline pairing, with the average number of votes it collects.
 * This is what tells which answer really lands on which setup.
 */
@Serializable
data class ComboView(
    val situationId: String,
    val situationText: String,
    val punchlineId: String,
    val punchlineText: String,
    val plays: Long,
    val votes: Long,
    val wins: Long,
    val voteRatio: Double,
)
