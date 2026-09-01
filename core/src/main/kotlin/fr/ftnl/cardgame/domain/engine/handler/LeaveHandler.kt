package fr.ftnl.cardgame.domain.engine.handler

import fr.ftnl.cardgame.domain.engine.CommandResult
import fr.ftnl.cardgame.domain.engine.GameCommand
import fr.ftnl.cardgame.domain.engine.GameError
import fr.ftnl.cardgame.domain.engine.GameEvent
import fr.ftnl.cardgame.domain.engine.RoundFlow
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.player.PlayerId

/**
 * Leaving outside a running match frees the seat; leaving a running game only marks the
 * player offline so their score and their answer of the round survive until it ends.
 */
internal class LeaveHandler(private val roundFlow: RoundFlow) {

    fun handle(state: GameState, command: GameCommand.Leave): CommandResult {
        if (!state.contains(command.playerId)) return CommandResult.rejected(GameError.UNKNOWN_PLAYER)
        val next = if (state.isMidGame) {
            disconnect(state, command.playerId)
        } else {
            state.withoutPlayers(setOf(command.playerId))
        }
        return roundFlow.advance(next, listOf(GameEvent.PlayerLeft(command.playerId)))
    }

    private fun disconnect(state: GameState, playerId: PlayerId): GameState = state.copy(
        players = state.players.map { if (it.id == playerId) it.copy(connected = false) else it },
    )
}
