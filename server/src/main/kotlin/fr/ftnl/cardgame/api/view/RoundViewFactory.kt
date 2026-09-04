package fr.ftnl.cardgame.api.view

import fr.ftnl.cardgame.api.dto.AnswerView
import fr.ftnl.cardgame.api.dto.ChatVotesView
import fr.ftnl.cardgame.api.dto.ChatVoterView
import fr.ftnl.cardgame.api.dto.RoundOutcomeView
import fr.ftnl.cardgame.api.dto.RoundView
import fr.ftnl.cardgame.api.dto.SituationCardView
import fr.ftnl.cardgame.domain.card.CardOrigin
import fr.ftnl.cardgame.domain.card.SituationCard
import fr.ftnl.cardgame.domain.game.ChatVoteTally
import fr.ftnl.cardgame.domain.game.GamePhase
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.game.Round
import fr.ftnl.cardgame.domain.game.RoundOutcome
import fr.ftnl.cardgame.domain.game.Submission
import fr.ftnl.cardgame.domain.game.SubmissionId
import fr.ftnl.cardgame.domain.player.PlayerId

/**
 * Projects the current round. Answers stay hidden while players write them, appear
 * anonymously during the vote, and are only tied back to their author at the reveal.
 */
class RoundViewFactory {

    fun create(state: GameState, viewer: PlayerId): RoundView? {
        val round = state.round ?: return null
        return RoundView(
            number = round.number,
            situation = situationView(round.situation),
            expectedAnswers = state.expectedAnswers,
            czarId = round.czarId?.value,
            answers = answers(state, round, viewer),
            myVote = round.votes[viewer]?.index,
            outcome = round.outcome?.let(::outcomeView),
        )
    }

    private fun answers(state: GameState, round: Round, viewer: PlayerId): List<AnswerView> {
        if (state.phase == GamePhase.LOBBY || state.phase == GamePhase.SUBMITTING) return emptyList()
        val revealAuthors = state.phase != GamePhase.SELECTING
        return round.revealed.map { (id, submission) ->
            answerView(round, id, submission, viewer, revealAuthors)
        }
    }

    private fun answerView(
        round: Round,
        id: SubmissionId,
        submission: Submission,
        viewer: PlayerId,
        revealAuthors: Boolean,
    ) = AnswerView(
        id = id.index,
        texts = submission.answers,
        filledText = round.situation.text.fill(submission.answers),
        authorId = submission.playerId.value.takeIf { revealAuthors },
        votes = round.outcome?.voteCounts?.get(id).takeIf { revealAuthors },
        isMine = submission.playerId == viewer,
        chatVotes = round.chatVotes[id]?.let(::chatVotesView),
    )

    private fun chatVotesView(tally: ChatVoteTally) = ChatVotesView(
        count = tally.count,
        voters = tally.voters.map { ChatVoterView(it.id, it.name, it.avatarUrl) },
    )

    private fun situationView(card: SituationCard) = SituationCardView(
        id = card.id.value,
        text = card.text.raw,
        blankCount = card.blankCount,
        custom = card.origin == CardOrigin.CUSTOM,
    )

    private fun outcomeView(outcome: RoundOutcome) = RoundOutcomeView(
        points = outcome.points.mapKeys { it.key.value },
        winners = outcome.winners.map { it.value },
        topAnswerId = outcome.topSubmission?.index,
    )
}
