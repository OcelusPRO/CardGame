package fr.ftnl.cardgame.domain.engine.handler

import fr.ftnl.cardgame.domain.engine.CommandResult
import fr.ftnl.cardgame.domain.engine.GameCommand
import fr.ftnl.cardgame.domain.engine.GameError
import fr.ftnl.cardgame.domain.engine.GameEvent
import fr.ftnl.cardgame.domain.engine.RoundFlow
import fr.ftnl.cardgame.domain.game.GameState

/** Flips the online flag when a socket drops or comes back, then unblocks the round. */
internal class ConnectionHandler(private val roundFlow: RoundFlow) {

    fun handle(state: GameState, command: GameCommand.SetConnected): CommandResult {
        if (!state.contains(command.playerId)) return CommandResult.rejected(GameError.UNKNOWN_PLAYER)
        val next = state.copy(
            players = state.players.map {
                if (it.id == command.playerId) it.copy(connected = command.connected) else it
            },
        )
        return roundFlow.advance(next, listOf(GameEvent.ConnectionChanged(command.playerId, command.connected)))
    }
}
