package fr.ftnl.cardgame.api.dto

import kotlinx.serialization.Serializable

/** Points of the round, ready to be animated by the client. */
@Serializable
data class RoundOutcomeView(
    val points: Map<String, Int>,
    val winners: List<String>,
    /** The answer to put on stage at the reveal, and the one that earned any bonus. */
    val topAnswerId: Int? = null,
)
