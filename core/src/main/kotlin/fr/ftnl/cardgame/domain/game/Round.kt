package fr.ftnl.cardgame.domain.game

import fr.ftnl.cardgame.domain.card.SituationCard
import fr.ftnl.cardgame.domain.player.PlayerId
import kotlinx.serialization.Serializable

/**
 * A single round: one situation, the answers it collected and how they were judged.
 * Answers stay keyed by player while they are secret, and are exposed through
 * [revealed] in a shuffled order once the submission step closes.
 *
 * [chatVotes] holds, per answer, what the Twitch chats gave it: one voice per viewer,
 * plus the first faces to show under it. It is a live tally, refreshed by the server
 * while the vote is open.
 */
@Serializable
data class Round(
    val number: Int,
    val situation: SituationCard,
    val czarId: PlayerId? = null,
    val submissions: Map<PlayerId, Submission> = emptyMap(),
    val revealOrder: List<PlayerId> = emptyList(),
    val votes: Map<PlayerId, SubmissionId> = emptyMap(),
    val chatVotes: Map<SubmissionId, ChatVoteTally> = emptyMap(),
    val outcome: RoundOutcome? = null,
) {
    init {
        require(number >= 1) { "A round number starts at 1" }
    }

    /** The answers paired with the anonymous handle used during the selection step. */
    val revealed: List<Pair<SubmissionId, Submission>>
        get() = revealOrder.mapIndexedNotNull { index, player ->
            submissions[player]?.let { SubmissionId(index) to it }
        }

    fun authorOf(submissionId: SubmissionId): PlayerId? = revealOrder.getOrNull(submissionId.index)

    fun handleOf(playerId: PlayerId): SubmissionId? =
        revealOrder.indexOf(playerId).takeIf { it >= 0 }?.let(::SubmissionId)

    fun hasSubmitted(playerId: PlayerId): Boolean = submissions.containsKey(playerId)

    fun hasVoted(playerId: PlayerId): Boolean = votes.containsKey(playerId)

    fun withSubmission(submission: Submission): Round =
        copy(submissions = submissions + (submission.playerId to submission))

    fun withVote(voter: PlayerId, choice: SubmissionId): Round =
        copy(votes = votes + (voter to choice))

    fun revealedInOrder(order: List<PlayerId>): Round = copy(revealOrder = order)

    /** Replaces the whole live tally with the snapshot the chat reader just produced. */
    fun withChatVotes(tallies: Map<SubmissionId, ChatVoteTally>): Round = copy(chatVotes = tallies)

    /** How many viewers picked each answer, every watched chat taken together. */
    val chatTally: Map<SubmissionId, Int>
        get() = chatVotes.mapValues { (_, tally) -> tally.count }

    /** Voices coming from the chats this round, all answers taken together. */
    val chatVoiceCount: Int get() = chatVotes.values.sumOf { it.count }
}
