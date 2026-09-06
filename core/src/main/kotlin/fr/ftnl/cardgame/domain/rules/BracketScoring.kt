package fr.ftnl.cardgame.domain.rules

import fr.ftnl.cardgame.domain.game.GameSettings
import fr.ftnl.cardgame.domain.game.Round
import fr.ftnl.cardgame.domain.game.RoundOutcome
import fr.ftnl.cardgame.domain.game.SubmissionId
import fr.ftnl.cardgame.domain.player.PlayerId

/**
 * The ladder pays by the duel: **one point per duel won**, `pointsPerVote` being what a
 * single won duel is worth so a table can still turn the dial.
 *
 * It applies whoever was voting: a table, a rotating czar, or a chat of four thousand. The
 * ladder counts duels won, and a duel is won the same way in every case.
 *
 * An answer that goes out in the first pairing scores nothing, one that survives to the
 * final scores every step of the way, and the champion is simply the one that never lost.
 * Neither a bye nor a tie counts — nobody was beaten — so a bracket of five answers hands
 * out fewer points than a bracket of eight, which is exactly right: there were fewer duels.
 *
 * There is no unanimity bonus here. A duel is already a yes-or-no on two answers, and
 * paying a landslide twice would just reward being lucky with the draw.
 */
class BracketScoring : RoundScoring {

    override fun score(round: Round, settings: GameSettings): RoundOutcome {
        val bracket = round.bracket ?: return RoundOutcome()
        val counts = round.revealed.associate { (id, _) -> id to bracket.winsOf(id) }
        return RoundOutcome(
            points = pointsPerAuthor(round, counts, settings),
            winners = listOfNotNull(bracket.champion?.let(round::authorOf)),
            voteCounts = counts,
            topSubmission = bracket.champion,
        )
    }

    private fun pointsPerAuthor(
        round: Round,
        counts: Map<SubmissionId, Int>,
        settings: GameSettings,
    ): Map<PlayerId, Int> = counts.mapNotNull { (submission, duelsWon) ->
        val author = round.authorOf(submission) ?: return@mapNotNull null
        val gain = duelsWon * settings.scoring.pointsPerVote
        (author to gain).takeIf { gain > 0 }
    }.toMap()
}
