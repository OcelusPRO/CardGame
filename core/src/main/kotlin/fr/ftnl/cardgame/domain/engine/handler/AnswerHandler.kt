package fr.ftnl.cardgame.domain.engine.handler

import fr.ftnl.cardgame.domain.card.PunchlineCard
import fr.ftnl.cardgame.domain.engine.CommandResult
import fr.ftnl.cardgame.domain.engine.GameCommand
import fr.ftnl.cardgame.domain.engine.GameError
import fr.ftnl.cardgame.domain.engine.GameEvent
import fr.ftnl.cardgame.domain.engine.RoundFlow
import fr.ftnl.cardgame.domain.game.AnswerMode
import fr.ftnl.cardgame.domain.game.GamePhase
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.game.Round
import fr.ftnl.cardgame.domain.game.Submission
import fr.ftnl.cardgame.domain.player.PlayerId

/** Registers the answer of a player, whether it comes from their hand or their keyboard. */
internal class AnswerHandler(private val roundFlow: RoundFlow) {

    fun playCards(state: GameState, command: GameCommand.PlayCards): CommandResult {
        val round = state.round ?: return CommandResult.rejected(GameError.WRONG_PHASE)
        validate(state, round, command.playerId, AnswerMode.CARDS, command.cardIds.size)
            ?.let { return CommandResult.rejected(it) }
        val hand = state.handOf(command.playerId)
        val played = pick(hand, command) ?: return CommandResult.rejected(GameError.CARD_NOT_IN_HAND)
        val fills = alignedFills(played, command.fills)
            ?: return CommandResult.rejected(GameError.WRONG_BLANK_COUNT)
        return submit(
            state.copy(hands = state.hands + (command.playerId to hand - played.toSet())),
            round,
            Submission(command.playerId, cards = played, cardFills = fills),
        )
    }

    /**
     * Trims the typed fills and checks each played card got exactly as many as it has
     * holes. Returns an empty list when no card needed filling, so a plain round stays
     * plain, or null when the count is off.
     */
    private fun alignedFills(
        cards: List<PunchlineCard>,
        fills: List<List<String>>,
    ): List<List<String>>? {
        if (cards.all { it.blankCount == 0 }) return emptyList()
        val aligned = cards.mapIndexed { index, card ->
            fills.getOrElse(index) { emptyList() }.map { it.trim() }
        }
        aligned.forEachIndexed { index, entry ->
            val expected = cards[index].blankCount
            if (entry.size != expected) return null
            if (entry.any { it.isEmpty() || it.length > MAX_ANSWER_LENGTH }) return null
        }
        return aligned
    }

    fun writeAnswers(state: GameState, command: GameCommand.WriteAnswers): CommandResult {
        val round = state.round ?: return CommandResult.rejected(GameError.WRONG_PHASE)
        validate(state, round, command.playerId, AnswerMode.FREE_TEXT, command.texts.size)
            ?.let { return CommandResult.rejected(it) }
        val texts = command.texts.map { it.trim() }
        if (texts.any { it.isEmpty() || it.length > MAX_ANSWER_LENGTH }) {
            return CommandResult.rejected(GameError.INVALID_ANSWER)
        }
        return submit(state, round, Submission(command.playerId, texts = texts))
    }

    private fun submit(state: GameState, round: Round, submission: Submission): CommandResult =
        roundFlow.advance(
            state.copy(round = round.withSubmission(submission)),
            listOf(GameEvent.AnswerSubmitted(submission.playerId)),
        )

    /** Resolves the played ids against the hand, refusing unknown or duplicated cards. */
    private fun pick(hand: List<PunchlineCard>, command: GameCommand.PlayCards): List<PunchlineCard>? {
        if (command.cardIds.distinct().size != command.cardIds.size) return null
        val byId = hand.associateBy { it.id }
        return command.cardIds.map { byId[it] ?: return null }
    }

    private fun validate(
        state: GameState,
        round: Round,
        playerId: PlayerId,
        mode: AnswerMode,
        answerCount: Int,
    ): GameError? = when {
        state.phase != GamePhase.SUBMITTING -> GameError.WRONG_PHASE
        state.settings.answerMode != mode -> GameError.WRONG_PHASE
        !state.contains(playerId) -> GameError.UNKNOWN_PLAYER
        round.czarId == playerId && !state.czarAnswers -> GameError.CZAR_CANNOT_ANSWER
        round.hasSubmitted(playerId) -> GameError.ALREADY_SUBMITTED
        answerCount != state.expectedAnswers -> GameError.WRONG_ANSWER_COUNT
        else -> null
    }

    private companion object {
        const val MAX_ANSWER_LENGTH = 120
    }
}
