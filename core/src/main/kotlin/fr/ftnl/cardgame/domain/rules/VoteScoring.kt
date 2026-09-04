package fr.ftnl.cardgame.domain.rules

import fr.ftnl.cardgame.domain.game.GameSettings
import fr.ftnl.cardgame.domain.game.Round
import fr.ftnl.cardgame.domain.game.RoundOutcome
import fr.ftnl.cardgame.domain.game.SubmissionId
import fr.ftnl.cardgame.domain.player.PlayerId

/**
 * Everybody votes: an answer earns `pointsPerVote` for each vote it collected, and
 * `unanimityBonus` on top when *everyone who could vote for it* did. A single voter
 * picking something else cancels that bonus.
 *
 * "Everyone who could" leaves the author out, since they are normally barred from voting
 * for themselves; with self voting allowed they count like anybody else.
 *
 * A Twitch chat reading the table is one more voice, see [Round.chatVoices]: it counts
 * exactly like a player vote, both for the points and for the unanimity.
 */
class VoteScoring : RoundScoring {

    override fun score(round: Round, settings: GameSettings): RoundOutcome {
        val counts = countVotes(round)
        val unanimous = counts.keys.filterTo(mutableSetOf()) { isUnanimous(round, it, settings) }
        return RoundOutcome(
            points = pointsPerAuthor(round, counts, unanimous, settings),
            winners = winnersOf(round, counts),
            voteCounts = counts,
            topSubmission = bestOf(counts.filterKeys { it in unanimous }) ?: bestOf(counts),
        )
    }

    /** Every voice of the round: one per player who voted, one per chat that spoke. */
    private fun countVotes(round: Round): Map<SubmissionId, Int> {
        val voices = round.votes.values + round.chatVoices
        return round.revealed.associate { (id, _) -> id to voices.count { it == id } }
    }

    private fun isUnanimous(round: Round, submission: SubmissionId, settings: GameSettings): Boolean {
        val author = round.authorOf(submission)
        val eligible = round.votes.filterKeys { settings.allowSelfVote || it != author }.values +
            round.chatVoices
        return eligible.isNotEmpty() && eligible.all { it == submission }
    }

    private fun bestOf(counts: Map<SubmissionId, Int>): SubmissionId? =
        counts.entries.filter { it.value > 0 }.maxByOrNull { it.value }?.key

    private fun pointsPerAuthor(
        round: Round,
        counts: Map<SubmissionId, Int>,
        unanimous: Set<SubmissionId>,
        settings: GameSettings,
    ): Map<PlayerId, Int> = counts.mapNotNull { (submission, votes) ->
        val author = round.authorOf(submission) ?: return@mapNotNull null
        val bonus = if (submission in unanimous) settings.scoring.unanimityBonus else 0
        val gain = votes * settings.scoring.pointsPerVote + bonus
        (author to gain).takeIf { gain > 0 }
    }.toMap()

    private fun winnersOf(round: Round, counts: Map<SubmissionId, Int>): List<PlayerId> {
        val best = counts.values.maxOrNull() ?: 0
        if (best == 0) return emptyList()
        return counts.filterValues { it == best }.keys.mapNotNull(round::authorOf)
    }
}
