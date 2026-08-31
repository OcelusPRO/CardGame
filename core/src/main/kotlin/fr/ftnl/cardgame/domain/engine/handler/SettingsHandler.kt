package fr.ftnl.cardgame.domain.engine.handler

import fr.ftnl.cardgame.domain.engine.CommandResult
import fr.ftnl.cardgame.domain.engine.GameCommand
import fr.ftnl.cardgame.domain.engine.GameError
import fr.ftnl.cardgame.domain.engine.GameEvent
import fr.ftnl.cardgame.domain.game.GamePhase
import fr.ftnl.cardgame.domain.game.GameState

/** Applies the host tuning, refused once the first card has been dealt. */
internal class SettingsHandler {

    fun handle(state: GameState, command: GameCommand.UpdateSettings): CommandResult {
        if (!state.isHost(command.by)) return CommandResult.rejected(GameError.NOT_THE_HOST)
        if (state.phase != GamePhase.LOBBY) return CommandResult.rejected(GameError.WRONG_PHASE)
        return CommandResult.accepted(state.copy(settings = command.settings), GameEvent.SettingsUpdated)
    }
}
