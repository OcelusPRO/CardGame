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
import fr.ftnl.cardgame.domain.player.PlayerId

/** Records a vote, or the pick of the card czar depending on the selected mode. */
internal class ChoiceHandler(private val roundFlow: RoundFlow) {

    fun handle(state: GameState, command: GameCommand.Choose): CommandResult {
        val round = state.round ?: return CommandResult.rejected(GameError.WRONG_PHASE)
        if (state.phase != GamePhase.SELECTING) return CommandResult.rejected(GameError.WRONG_PHASE)
        if (!state.contains(command.playerId)) return CommandResult.rejected(GameError.UNKNOWN_PLAYER)
        val author = round.authorOf(command.submissionId)
            ?: return CommandResult.rejected(GameError.UNKNOWN_SUBMISSION)
        eligibility(state, round, command.playerId, author)?.let { return CommandResult.rejected(it) }
        return roundFlow.advance(
            state.copy(round = round.withVote(command.playerId, command.submissionId)),
            listOf(GameEvent.ChoiceMade(command.playerId)),
        )
    }

    private fun eligibility(
        state: GameState,
        round: Round,
        voter: PlayerId,
        author: PlayerId,
    ): GameError? = when {
        round.hasVoted(voter) -> GameError.ALREADY_VOTED
        state.settings.selectionMode == SelectionMode.CZAR && round.czarId != voter -> GameError.NOT_THE_CZAR
        state.settings.selectionMode == SelectionMode.CZAR && author == voter -> GameError.CANNOT_VOTE_OWN_ANSWER
        state.settings.selectionMode == SelectionMode.VOTE &&
            author == voter &&
            !state.settings.allowSelfVote -> GameError.CANNOT_VOTE_OWN_ANSWER
        else -> null
    }
}
