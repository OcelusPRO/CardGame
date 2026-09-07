package fr.ftnl.cardgame.domain.engine.handler

import fr.ftnl.cardgame.domain.engine.CommandResult
import fr.ftnl.cardgame.domain.engine.GameCommand
import fr.ftnl.cardgame.domain.engine.GameError
import fr.ftnl.cardgame.domain.engine.GameEvent
import fr.ftnl.cardgame.domain.engine.RoundFlow
import fr.ftnl.cardgame.domain.game.GamePhase
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.game.Round
import fr.ftnl.cardgame.domain.game.SelectionMode
import fr.ftnl.cardgame.domain.game.SubmissionId
import fr.ftnl.cardgame.domain.player.PlayerId

/**
 * Records a vote, or the pick of the card czar depending on who is judging.
 *
 * Two questions are asked, in that order and independently, because they are independent
 * settings: **may this player vote at all** — the chat judging alone rules the table out,
 * a czar rules everybody but themselves out — and then **may they cast this particular
 * vote right now**, which is where the ladder narrows the choice down to two answers.
 */
internal class ChoiceHandler(private val roundFlow: RoundFlow) {

    fun handle(state: GameState, command: GameCommand.Choose): CommandResult {
        val round = state.round ?: return CommandResult.rejected(GameError.WRONG_PHASE)
        if (state.phase != GamePhase.SELECTING) return CommandResult.rejected(GameError.WRONG_PHASE)
        if (!state.contains(command.playerId)) return CommandResult.rejected(GameError.UNKNOWN_PLAYER)
        val author = round.authorOf(command.submissionId)
            ?: return CommandResult.rejected(GameError.UNKNOWN_SUBMISSION)
        refusal(state, round, command.playerId, author, command.submissionId)
            ?.let { return CommandResult.rejected(it) }
        val voted = if (state.settings.runsBracket) {
            round.withDuelVote(command.playerId, command.submissionId)
        } else {
            round.withVote(command.playerId, command.submissionId)
        }
        return roundFlow.advance(
            state.copy(round = voted),
            listOf(GameEvent.ChoiceMade(command.playerId)),
        )
    }

    private fun refusal(
        state: GameState,
        round: Round,
        voter: PlayerId,
        author: PlayerId,
        choice: SubmissionId,
    ): GameError? = whoMayVote(state, round, voter)
        ?: whatMayBeVotedOn(state, round, voter, author, choice)

    /** Settled by [SelectionMode] alone: it is the same answer whatever the format. */
    private fun whoMayVote(state: GameState, round: Round, voter: PlayerId): GameError? = when {
        state.settings.selectionMode == SelectionMode.CHAT -> GameError.ONLY_THE_CHAT_VOTES
        state.settings.selectionMode == SelectionMode.CZAR && round.czarId != voter ->
            GameError.NOT_THE_CZAR
        else -> null
    }

    /**
     * Settled by the format, plus the rule both formats share: your own card is not yours
     * to pick unless the table says otherwise. A ladder adds that only the two answers
     * currently facing off are on the table at all, and that a vote is spent per duel
     * rather than per round — so the same player votes again, on the next pair, a few
     * seconds later.
     */
    private fun whatMayBeVotedOn(
        state: GameState,
        round: Round,
        voter: PlayerId,
        author: PlayerId,
        choice: SubmissionId,
    ): GameError? {
        if (state.hasVoted(voter)) return GameError.ALREADY_VOTED
        val duel = if (state.settings.runsBracket) {
            round.bracket?.current ?: return GameError.WRONG_PHASE
        } else {
            null
        }
        if (duel != null && !duel.holds(choice)) return GameError.NOT_IN_THIS_DUEL
        // Who may crown their own card is settled by the state, because it is not the same
        // answer for a czar as for the table — see [GameState.allowsSelfVote].
        if (state.allowsSelfVote(voter)) return null
        if (author == voter) return GameError.CANNOT_VOTE_OWN_ANSWER
        if (duel != null && voter in round.authorsOf(duel)) return GameError.CANNOT_JUDGE_OWN_DUEL
        return null
    }
}
