package fr.ftnl.cardgame.domain.rules

import fr.ftnl.cardgame.domain.game.GameSettings
import fr.ftnl.cardgame.domain.game.Round
import fr.ftnl.cardgame.domain.game.RoundOutcome

/**
 * A rotating card czar picks a single winner. Their pick is one approving voice, so it is
 * worth the same `pointsPerVote` as a vote would be.
 */
class CzarScoring : RoundScoring {

    override fun score(round: Round, settings: GameSettings): RoundOutcome {
        val pick = round.czarId?.let { round.votes[it] } ?: return RoundOutcome()
        val author = round.authorOf(pick) ?: return RoundOutcome()
        return RoundOutcome(
            points = mapOf(author to settings.scoring.pointsPerVote),
            winners = listOf(author),
            voteCounts = round.revealed.associate { (id, _) -> id to if (id == pick) 1 else 0 },
            topSubmission = pick,
        )
    }
}
