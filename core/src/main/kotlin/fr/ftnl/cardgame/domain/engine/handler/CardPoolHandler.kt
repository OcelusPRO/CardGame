package fr.ftnl.cardgame.domain.engine.handler

import fr.ftnl.cardgame.domain.deck.DrawPile
import fr.ftnl.cardgame.domain.deck.Shuffler
import fr.ftnl.cardgame.domain.engine.CommandResult
import fr.ftnl.cardgame.domain.engine.GameCommand
import fr.ftnl.cardgame.domain.engine.GameError
import fr.ftnl.cardgame.domain.engine.GameEvent
import fr.ftnl.cardgame.domain.game.GamePhase
import fr.ftnl.cardgame.domain.game.GameState

/**
 * Loads the cards the game will draw from. The server resolves official decks and
 * player written cards beforehand, the domain only turns them into shuffled piles.
 */
internal class CardPoolHandler(private val shuffler: Shuffler) {

    fun handle(state: GameState, command: GameCommand.SetCardPool): CommandResult {
        if (!state.isHost(command.by)) return CommandResult.rejected(GameError.NOT_THE_HOST)
        if (state.phase != GamePhase.LOBBY) return CommandResult.rejected(GameError.WRONG_PHASE)
        return CommandResult.accepted(
            state.copy(
                situations = DrawPile.shuffled(command.pool.situations, shuffler),
                punchlines = DrawPile.shuffled(command.pool.punchlines, shuffler),
            ),
            GameEvent.SettingsUpdated,
        )
    }
}
