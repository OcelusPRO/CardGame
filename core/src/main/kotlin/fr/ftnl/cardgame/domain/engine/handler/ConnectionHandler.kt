package fr.ftnl.cardgame.domain.engine.handler

import fr.ftnl.cardgame.domain.engine.CommandResult
import fr.ftnl.cardgame.domain.engine.GameCommand
import fr.ftnl.cardgame.domain.engine.GameError
import fr.ftnl.cardgame.domain.engine.GameEvent
import fr.ftnl.cardgame.domain.engine.RoundFlow
import fr.ftnl.cardgame.domain.game.GameState

/**
 * Flips the online flag when a socket drops or comes back, then unblocks the round.
 *
 * A drop never frees the seat on its own: the player is only marked offline, and it is
 * [GameCommand.DropIfAway] (after the grace delay) or the end of the game that lets them
 * go, so a quick reconnection keeps their place.
 *
 * The crown follows the same rule: while a round is actually being played, a disconnected
 * host keeps it — a reload is a drop-and-reconnect, and swapping hosts mid-round would
 * hand control to somebody who has no idea what just changed, right as they need to act
 * on it. The scheduler runs the round regardless of who currently holds the crown, so
 * nothing is actually blocked by leaving it alone. Outside a round — the lobby, or a
 * finished game waiting on its host — the crown does move immediately, since there the
 * table would otherwise sit idle for no reason. See [RoundFlowHandler] for the one
 * handover that does happen mid-game: at the very end, if the original host never came
 * back.
 */
internal class ConnectionHandler(private val roundFlow: RoundFlow) {

    fun handle(state: GameState, command: GameCommand.SetConnected): CommandResult {
        if (!state.contains(command.playerId)) return CommandResult.rejected(GameError.UNKNOWN_PLAYER)

        val handedOver = !command.connected && command.playerId == state.hostId && !state.isMidGame
        val next = state.copy(
            players = state.players.map {
                if (it.id == command.playerId) it.copy(connected = command.connected) else it
            },
            hostId = if (handedOver) state.successorTo(state.hostId) ?: state.hostId else state.hostId,
        )
        return roundFlow.advance(next, listOf(GameEvent.ConnectionChanged(command.playerId, command.connected)))
    }
}
