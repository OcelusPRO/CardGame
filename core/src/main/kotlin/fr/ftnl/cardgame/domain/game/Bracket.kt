package fr.ftnl.cardgame.domain.game

import fr.ftnl.cardgame.domain.player.PlayerId
import kotlinx.serialization.Serializable

/**
 * Two answers head to head. [right] is null for a bye: an odd tier walks its odd one out
 * through to the next round, and a walkover is not a win.
 */
@Serializable
data class Duel(
    val left: SubmissionId,
    val right: SubmissionId? = null,
    val votes: Map<PlayerId, SubmissionId> = emptyMap(),
    val winner: SubmissionId? = null,
    /** True when the winner was actually chosen, as opposed to walking through a bye or a tie. */
    val earned: Boolean = false,
) {
    val isBye: Boolean get() = right == null

    val settled: Boolean get() = winner != null

    fun holds(submission: SubmissionId): Boolean = submission == left || submission == right

    fun hasVoted(playerId: PlayerId): Boolean = votes.containsKey(playerId)

    fun withVote(voter: PlayerId, choice: SubmissionId): Duel = copy(votes = votes + (voter to choice))

    /**
     * The voices for one side: those cast at the table, plus those the Twitch chats gave
     * it. A duel does not care where a voice came from — that is [SelectionMode]'s
     * business, decided long before the count.
     */
    fun countFor(submission: SubmissionId, chat: Map<SubmissionId, Int> = emptyMap()): Int =
        votes.values.count { it == submission } + (chat[submission] ?: 0)

    /**
     * Calls the duel. A bye advances on its own, and a tie — nobody voted, or both sides
     * drew level — sends the answer that was revealed first through **without** a point:
     * somebody has to move on, but nobody beat anybody.
     */
    fun decide(chat: Map<SubmissionId, Int> = emptyMap()): Duel {
        val right = right ?: return copy(winner = left, earned = false)
        val forLeft = countFor(left, chat)
        val forRight = countFor(right, chat)
        return when {
            forLeft > forRight -> copy(winner = left, earned = true)
            forRight > forLeft -> copy(winner = right, earned = true)
            else -> copy(winner = left, earned = false)
        }
    }
}

/**
 * The knockout ladder of a round, in [SelectionFormat.DUELS].
 *
 * A dozen answers in front of everybody at once turns the vote into a reading exercise
 * nobody finishes. Here they are paired instead: whoever holds the vote reads two, picks
 * one, and the winner moves up a tier until one is left. A round is therefore several short
 * votes rather than one long one, and a player scores for every duel their answer won.
 *
 * The ladder is indifferent to **who** is voting. A table, a rotating czar and a chat of
 * four thousand all reduce to a count on each side — which is what makes it a format that
 * pairs with every [SelectionMode] rather than a fourth one.
 *
 * Only the current tier is held: the next one is built from the winners of this one, so
 * the snapshot never carries a ladder of empty slots.
 */
@Serializable
data class Bracket(
    val tier: Int = 1,
    val duels: List<Duel> = emptyList(),
    /** Duels won outright, per answer, across every tier. This is what scores. */
    val wins: Map<SubmissionId, Int> = emptyMap(),
    val champion: SubmissionId? = null,
) {
    val isComplete: Boolean get() = champion != null

    /** The duel being judged right now, or null once the ladder is done. */
    val current: Duel? get() = duels.firstOrNull { !it.settled }

    /** Where the table is in the current tier, one-based, for a "duel 2 / 4" caption. */
    val currentNumber: Int get() = duels.indexOfFirst { !it.settled }.let { if (it < 0) duels.size else it + 1 }

    fun winsOf(submission: SubmissionId): Int = wins[submission] ?: 0

    fun withVote(voter: PlayerId, choice: SubmissionId): Bracket {
        val index = duels.indexOfFirst { !it.settled }
        if (index < 0) return this
        return copy(duels = duels.replacing(index) { it.withVote(voter, choice) })
    }

    /** Calls the open duel, banks the point it was worth, and opens whatever comes next. */
    fun settleCurrent(chat: Map<SubmissionId, Int> = emptyMap()): Bracket {
        val index = duels.indexOfFirst { !it.settled }
        if (index < 0) return this
        val decided = duels[index].decide(chat)
        val winner = decided.winner ?: return this
        return copy(
            duels = duels.replacing(index) { decided },
            wins = if (decided.earned) wins + (winner to winsOf(winner) + 1) else wins,
        ).openNextTier()
    }

    private fun openNextTier(): Bracket {
        if (duels.any { !it.settled }) return this
        val survivors = duels.mapNotNull { it.winner }
        if (survivors.size <= 1) return copy(champion = survivors.firstOrNull())
        return copy(tier = tier + 1, duels = pairUp(survivors))
    }

    private fun List<Duel>.replacing(index: Int, change: (Duel) -> Duel): List<Duel> =
        mapIndexed { position, duel -> if (position == index) change(duel) else duel }

    companion object {
        /**
         * Opens the ladder on the answers of a round, in the order they were revealed —
         * which is already shuffled, so the pairing gives nothing away.
         */
        fun of(entrants: List<SubmissionId>): Bracket = when {
            entrants.isEmpty() -> Bracket()
            entrants.size == 1 -> Bracket(champion = entrants.first())
            else -> Bracket(duels = pairUp(entrants))
        }

        private fun pairUp(entrants: List<SubmissionId>): List<Duel> =
            entrants.chunked(2).map { pair ->
                if (pair.size == 2) Duel(pair[0], pair[1]) else Duel(pair[0], winner = pair[0])
            }
    }
}
