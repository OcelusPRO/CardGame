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
