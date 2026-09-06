package fr.ftnl.cardgame.domain.engine.handler

import fr.ftnl.cardgame.domain.card.PunchlineCard
import fr.ftnl.cardgame.domain.card.SituationCard
import fr.ftnl.cardgame.domain.deck.Shuffler
import fr.ftnl.cardgame.domain.engine.CommandResult
import fr.ftnl.cardgame.domain.engine.GameCommand
import fr.ftnl.cardgame.domain.engine.GameError
import fr.ftnl.cardgame.domain.engine.GameEvent
import fr.ftnl.cardgame.domain.game.ChatCardSettings
import fr.ftnl.cardgame.domain.game.GamePhase
import fr.ftnl.cardgame.domain.game.GameState

/**
 * Takes the cards a Twitch chat wrote and slips them into the piles the game draws from.
 *
 * They go in shuffled and mid-game on purpose: a situation the chat just typed is not the
 * very next one dealt, so nobody can time a proposal to hit a particular round. Whether
 * the viewer had to pay for it — channel points, bits, or nothing at all — was settled
 * before this point, by the chat reader that saw the message and its Twitch tags.
 */
internal class ChatCardHandler(private val shuffler: Shuffler) {

    fun handle(state: GameState, command: GameCommand.AddChatCards): CommandResult {
        val rules = state.settings.chatCards
        if (!rules.enabled) return CommandResult.rejected(GameError.CHAT_CARDS_CLOSED)
        if (state.phase == GamePhase.FINISHED) return CommandResult.rejected(GameError.WRONG_PHASE)

        val room = ChatCardSettings.MAX_CARDS_PER_GAME - state.chatCardCount
        if (room <= 0) return CommandResult.rejected(GameError.CHAT_CARDS_FULL)

        val situations = if (rules.situations) command.situations.take(room) else emptyList()
        val punchlines =
            if (rules.punchlines) command.punchlines.take(room - situations.size) else emptyList()
        if (situations.isEmpty() && punchlines.isEmpty()) {
            return CommandResult.rejected(GameError.CHAT_CARDS_CLOSED)
        }
        return CommandResult.accepted(
            grow(state, situations, punchlines),
            GameEvent.ChatCardsAdded(situations.size, punchlines.size),
        )
    }

    private fun grow(
        state: GameState,
        situations: List<SituationCard>,
        punchlines: List<PunchlineCard>,
    ): GameState = state.copy(
        situations = state.situations.addShuffled(situations, shuffler),
        punchlines = state.punchlines.addShuffled(punchlines, shuffler),
    )
}
