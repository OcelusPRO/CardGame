package fr.ftnl.cardgame.domain.engine.handler

import fr.ftnl.cardgame.domain.engine.CommandResult
import fr.ftnl.cardgame.domain.engine.GameCommand
import fr.ftnl.cardgame.domain.engine.GameEvent
import fr.ftnl.cardgame.domain.engine.RoundFlow
import fr.ftnl.cardgame.domain.game.GameState

/**
 * Runs when the disconnect grace delay is over. The seat is freed only if it is still
 * worth freeing: the player has not reconnected, a match is not actually running, and the
 * game has not already ended. Anything else is a no-op, so a player who came back keeps
 * their place — and one who is only missing from a finished game's recap stays in it,
 * score and all, rather than quietly vanishing five seconds after the last round.
 */
internal class DropIfAwayHandler(private val roundFlow: RoundFlow) {

    fun handle(state: GameState, command: GameCommand.DropIfAway): CommandResult {
        val player = state.playerOf(command.playerId) ?: return CommandResult.Accepted(state, emptyList())
        if (player.connected || state.isMidGame || state.isOver) return CommandResult.Accepted(state, emptyList())
        val next = state.withoutPlayers(setOf(command.playerId))
        return roundFlow.advance(next, listOf(GameEvent.PlayerLeft(command.playerId)))
    }
}
