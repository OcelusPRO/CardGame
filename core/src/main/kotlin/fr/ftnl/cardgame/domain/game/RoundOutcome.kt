package fr.ftnl.cardgame.domain.game

import fr.ftnl.cardgame.domain.player.PlayerId
import kotlinx.serialization.Serializable

/** Points handed out at the end of a round, plus the detail needed to animate the reveal. */
@Serializable
data class RoundOutcome(
    val points: Map<PlayerId, Int> = emptyMap(),
    val winners: List<PlayerId> = emptyList(),
    val voteCounts: Map<SubmissionId, Int> = emptyMap(),
    /** The answer to put on stage at the reveal, and the one that earned any bonus. */
    val topSubmission: SubmissionId? = null,
)
