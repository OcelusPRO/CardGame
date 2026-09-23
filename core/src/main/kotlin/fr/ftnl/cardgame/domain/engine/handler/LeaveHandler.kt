package fr.ftnl.cardgame.domain.engine.handler

import fr.ftnl.cardgame.domain.engine.CommandResult
import fr.ftnl.cardgame.domain.engine.GameCommand
import fr.ftnl.cardgame.domain.engine.GameError
import fr.ftnl.cardgame.domain.engine.GameEvent
import fr.ftnl.cardgame.domain.engine.RoundFlow
import fr.ftnl.cardgame.domain.game.GamePhase
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.player.PlayerId

/**
 * Leaving outside a running match frees the seat; leaving a running game only marks the
 * player offline so their score and their answer of the round survive until it ends.
 */
internal class LeaveHandler(private val roundFlow: RoundFlow) {

    fun handle(state: GameState, command: GameCommand.Leave): CommandResult {
        if (!state.contains(command.playerId)) return CommandResult.rejected(GameError.UNKNOWN_PLAYER)
        // The device of a shared table is the table: when it walks away, so does every
        // seat around it, and the game is left to be forgotten like any deserted one.
        if (command.playerId == state.deviceOwner) {
            return CommandResult.Accepted(
                state.withoutPlayers(state.players.map { it.id }.toSet()),
                state.players.map { GameEvent.PlayerLeft(it.id) },
            )
        }
        val next = if (state.isMidGame) {
            disconnect(state, command.playerId)
        } else {
            state.withoutPlayers(setOf(command.playerId))
        }
        return roundFlow.advance(next, listOf(GameEvent.PlayerLeft(command.playerId)))
    }

    /**
     * Swaps a seat for the stream page, in the lobby only — mid-match the seat carries a
     * hand and a score, and nothing about watching is worth throwing those away for.
     *
     * The host is the case this exists for: a streamer who shows the table on the computer
     * they stream from, and plays it on their phone under another account. The crown goes
     * to the [GameCommand.StepAside.heir] they name, not to whoever sat down first, since
     * the one seat that should run the table is the one they are about to pick up.
     */
    fun stepAside(state: GameState, command: GameCommand.StepAside): CommandResult {
        if (!state.contains(command.playerId)) return CommandResult.rejected(GameError.UNKNOWN_PLAYER)
        if (state.sharedDevice) return CommandResult.rejected(GameError.SHARED_DEVICE)
        if (state.phase != GamePhase.LOBBY) return CommandResult.rejected(GameError.WRONG_PHASE)
        val crowned = if (state.isHost(command.playerId)) {
            val heir = command.heir?.takeIf { it != command.playerId && state.contains(it) }
                ?: return CommandResult.rejected(GameError.UNKNOWN_PLAYER)
            state.copy(hostId = heir)
        } else {
            state
        }
        return CommandResult.accepted(
            crowned.withoutPlayers(setOf(command.playerId)),
            GameEvent.PlayerLeft(command.playerId),
        )
    }

    private fun disconnect(state: GameState, playerId: PlayerId): GameState = state.copy(
        players = state.players.map { if (it.id == playerId) it.copy(connected = false) else it },
    )
}
