package fr.ftnl.cardgame.domain.rules

import fr.ftnl.cardgame.domain.game.GameSettings
import fr.ftnl.cardgame.domain.game.Round
import fr.ftnl.cardgame.domain.game.RoundOutcome
import fr.ftnl.cardgame.domain.game.SubmissionId
import fr.ftnl.cardgame.domain.player.PlayerId

/**
 * The Twitch chats judge alone: the answer the viewers picked most takes the round, and
 * it is worth **one point**, whatever the size of the audience.
 *
 * Counting the voices themselves would make the score a headcount — four hundred viewers
 * against forty, and the game would be over by the second round. A round is a round: the
 * final score is simply how many of them a player carried. Nothing else is rewarded, so
 * neither the points per vote nor the unanimity bonus apply here.
 *
 * A tie hands the point to every answer that reached the top; a round nobody voted in
 * gives nothing to anybody.
 */
class ChatScoring : RoundScoring {

    override fun score(round: Round, settings: GameSettings): RoundOutcome {
        val counts = round.revealed.associate { (id, _) -> id to (round.chatTally[id] ?: 0) }
        val best = counts.values.maxOrNull() ?: 0
        if (best == 0) return RoundOutcome(voteCounts = counts)
        val top = counts.filterValues { it == best }.keys
        val winners: List<PlayerId> = top.mapNotNull(round::authorOf)
        return RoundOutcome(
            points = winners.associateWith { ROUND_POINT },
            winners = winners,
            voteCounts = counts,
            topSubmission = top.minByOrNull(SubmissionId::index),
        )
    }

    private companion object {
        /** Winning a round is worth exactly that, and a game is a count of rounds won. */
        const val ROUND_POINT = 1
    }
}
