package fr.ftnl.cardgame.domain.engine.handler

import fr.ftnl.cardgame.domain.engine.CommandResult
import fr.ftnl.cardgame.domain.engine.GameCommand
import fr.ftnl.cardgame.domain.engine.GameError
import fr.ftnl.cardgame.domain.engine.GameEvent
import fr.ftnl.cardgame.domain.engine.RoundFlow
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.player.PlayerId

/**
 * Flips the online flag when a socket drops or comes back, then unblocks the round.
 *
 * A drop never frees the seat on its own: the player is only marked offline, and it is
 * [GameCommand.DropIfAway] (after the grace delay) or the end of the game that lets them
 * go, so a quick reconnection keeps their place. The one thing that does move on a drop
 * is the crown: a disconnected host hands it to the next player in, so the table is
 * never left without someone able to run it.
 */
internal class ConnectionHandler(private val roundFlow: RoundFlow) {

    fun handle(state: GameState, command: GameCommand.SetConnected): CommandResult {
        if (!state.contains(command.playerId)) return CommandResult.rejected(GameError.UNKNOWN_PLAYER)

        val handedOver = !command.connected && command.playerId == state.hostId
        val next = state.copy(
            players = state.players.map {
                if (it.id == command.playerId) it.copy(connected = command.connected) else it
            },
            hostId = if (handedOver) nextHost(state) else state.hostId,
        )
        return roundFlow.advance(next, listOf(GameEvent.ConnectionChanged(command.playerId, command.connected)))
    }

    /** The earliest joined player who is not the leaving host, an online one for choice. */
    private fun nextHost(state: GameState): PlayerId {
        val others = state.players.filter { it.id != state.hostId }
        return others.firstOrNull { it.connected }?.id
            ?: others.firstOrNull()?.id
            ?: state.hostId
    }
}
