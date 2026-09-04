package fr.ftnl.cardgame.domain.game

import fr.ftnl.cardgame.domain.card.SituationCard
import fr.ftnl.cardgame.domain.player.PlayerId
import kotlinx.serialization.Serializable

/**
 * A single round: one situation, the answers it collected and how they were judged.
 * Answers stay keyed by player while they are secret, and are exposed through
 * [revealed] in a shuffled order once the submission step closes.
 *
 * [chatVotes] holds, per Twitch channel read for this table, how many viewers typed the
 * number of each answer. It is a live tally: the server pushes a fresh snapshot of it
 * while the vote is open, and it never carries a viewer name.
 */
@Serializable
data class Round(
    val number: Int,
    val situation: SituationCard,
    val czarId: PlayerId? = null,
    val submissions: Map<PlayerId, Submission> = emptyMap(),
    val revealOrder: List<PlayerId> = emptyList(),
    val votes: Map<PlayerId, SubmissionId> = emptyMap(),
    val chatVotes: Map<String, Map<SubmissionId, Int>> = emptyMap(),
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
    fun withChatVotes(tallies: Map<String, Map<SubmissionId, Int>>): Round =
        copy(chatVotes = tallies)

    /** How many viewers picked each answer, all watched chats taken together. */
    val chatTally: Map<SubmissionId, Int>
        get() = chatVotes.values.flatMap { it.entries }
            .groupingBy { it.key }
            .fold(0) { total, entry -> total + entry.value }

    /**
     * One voice per chat, given to the answer that chat preferred. A chat weighing as
     * much as its whole audience would drown the table out, so a community speaks with a
     * single voice — and a table of streamers gets one voice per community. A tie inside
     * a chat is an abstention.
     */
    val chatVoices: List<SubmissionId>
        get() = chatVotes.values.mapNotNull { tally ->
            val best = tally.values.maxOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
            tally.entries.filter { it.value == best }.singleOrNull()?.key
        }
}
