package fr.ftnl.cardgame.domain.engine.handler

import fr.ftnl.cardgame.domain.engine.CommandResult
import fr.ftnl.cardgame.domain.engine.GameCommand
import fr.ftnl.cardgame.domain.engine.GameError
import fr.ftnl.cardgame.domain.engine.GameEvent
import fr.ftnl.cardgame.domain.game.GamePhase
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.game.Scoreboard

/** Lets the host free a seat, in the lobby only, so a running round is never disturbed. */
internal class KickHandler {

    fun handle(state: GameState, command: GameCommand.Kick): CommandResult {
        if (!state.isHost(command.by)) return CommandResult.rejected(GameError.NOT_THE_HOST)
        if (state.phase != GamePhase.LOBBY) return CommandResult.rejected(GameError.WRONG_PHASE)
        if (command.by == command.playerId) return CommandResult.rejected(GameError.CANNOT_KICK_SELF)
        if (!state.contains(command.playerId)) return CommandResult.rejected(GameError.UNKNOWN_PLAYER)
        return CommandResult.accepted(
            state.copy(
                players = state.players.filterNot { it.id == command.playerId },
                hands = state.hands - command.playerId,
                scoreboard = Scoreboard(state.scoreboard.points - command.playerId),
            ),
            GameEvent.PlayerLeft(command.playerId),
        )
    }
}
