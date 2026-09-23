package fr.ftnl.cardgame.domain.engine.handler

import fr.ftnl.cardgame.domain.engine.CommandResult
import fr.ftnl.cardgame.domain.engine.GameCommand
import fr.ftnl.cardgame.domain.engine.GameError
import fr.ftnl.cardgame.domain.engine.GameEvent
import fr.ftnl.cardgame.domain.game.GamePhase
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.player.Player

/** Seats a new player, or brings a known one back online after a refresh. */
internal class JoinHandler {

    fun handle(state: GameState, command: GameCommand.Join): CommandResult {
        val player = command.player
        if (state.contains(player.id)) return reconnect(state, player)
        // Every seat of a shared device sits on the sofa next to it: the invitation link
        // leads nowhere, since there is no other phone to play from.
        if (state.sharedDevice) return CommandResult.rejected(GameError.SHARED_DEVICE)
        return seat(state, player)
    }

    /** One more player around the shared device, added by the device itself. */
    fun addSeat(state: GameState, command: GameCommand.AddSeat): CommandResult {
        // An online table has no owner to match, so nobody may add a seat to it this way.
        if (command.by != state.deviceOwner) return CommandResult.rejected(GameError.NOT_THE_HOST)
        if (state.contains(command.player.id)) return CommandResult.rejected(GameError.NICKNAME_TAKEN)
        return seat(state, command.player)
    }

    private fun seat(state: GameState, player: Player): CommandResult {
        if (state.phase != GamePhase.LOBBY) return CommandResult.rejected(GameError.GAME_ALREADY_STARTED)
        if (state.players.size >= state.settings.maxPlayers) return CommandResult.rejected(GameError.GAME_FULL)
        if (state.players.any { it.nickname == player.nickname }) {
            return CommandResult.rejected(GameError.NICKNAME_TAKEN)
        }
        return CommandResult.accepted(
            state.copy(
                players = state.players + player,
                scoreboard = state.scoreboard.withPlayer(player.id),
            ),
            GameEvent.PlayerJoined(player.id),
        )
    }

    private fun reconnect(state: GameState, player: Player): CommandResult = CommandResult.accepted(
        state.copy(players = state.players.map { if (it.id == player.id) player.copy(connected = true) else it }),
        GameEvent.ConnectionChanged(player.id, connected = true),
    )
}
