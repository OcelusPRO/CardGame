package fr.ftnl.cardgame.domain.engine.handler

import fr.ftnl.cardgame.domain.engine.CommandResult
import fr.ftnl.cardgame.domain.engine.GameCommand
import fr.ftnl.cardgame.domain.engine.GameEvent
import fr.ftnl.cardgame.domain.engine.RoundFlow
import fr.ftnl.cardgame.domain.game.GameState

/**
 * Runs when the disconnect grace delay is over. The seat is freed only if it is still
 * worth freeing: the player is gone for good (not mid-match) and has not reconnected.
 * Anything else is a no-op, so a player who came back keeps their place.
 */
internal class DropIfAwayHandler(private val roundFlow: RoundFlow) {

    fun handle(state: GameState, command: GameCommand.DropIfAway): CommandResult {
        val player = state.playerOf(command.playerId) ?: return CommandResult.Accepted(state, emptyList())
        if (player.connected || state.isMidGame) return CommandResult.Accepted(state, emptyList())
        val next = state.withoutPlayers(setOf(command.playerId))
        return roundFlow.advance(next, listOf(GameEvent.PlayerLeft(command.playerId)))
    }
}
