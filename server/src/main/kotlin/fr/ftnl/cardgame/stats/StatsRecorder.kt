package fr.ftnl.cardgame.stats

import fr.ftnl.cardgame.domain.card.CardOrigin
import fr.ftnl.cardgame.domain.engine.GameEvent
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.game.Round
import fr.ftnl.cardgame.game.GameListener

/**
 * Feeds the aggregated statistics from what games actually produce. Cards written by
 * players are skipped on purpose: they belong to their session, not to the site.
 */
class StatsRecorder(private val writer: UsageStatsWriter) : GameListener {

    override suspend fun onGameCreated(state: GameState) = writer.recordGameCreated()

    override suspend fun onGameChanged(state: GameState, events: List<GameEvent>) {
        events.filterIsInstance<GameEvent.HandsRefilled>()
            .flatMap { it.punchlineCardIds }
            .takeIf { it.isNotEmpty() }
            ?.let { writer.recordDeals(it) }
        events.filterIsInstance<GameEvent.RoundEnded>().forEach { record(it.round) }
    }

    private suspend fun record(round: Round) {
        val outcome = round.outcome ?: return
        val answers = round.revealed.flatMap { (submissionId, submission) ->
            val votes = outcome.voteCounts[submissionId] ?: 0
            val won = submission.playerId in outcome.winners
            submission.cards.filter { it.origin == CardOrigin.OFFICIAL }
                .map { PunchlineUsage(it.id, votes, won) }
        }
        writer.recordRound(RoundUsage(situationId = officialId(round), answers = answers))
    }

    private fun officialId(round: Round) =
        round.situation.id.takeIf { round.situation.origin == CardOrigin.OFFICIAL }
}
