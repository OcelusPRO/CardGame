package fr.ftnl.cardgame.domain.engine.handler

import fr.ftnl.cardgame.domain.engine.CommandResult
import fr.ftnl.cardgame.domain.engine.GameCommand
import fr.ftnl.cardgame.domain.engine.GameError
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.player.Player

/**
 * Remembers the Twitch account of a seated player. A player often signs in *after*
 * sitting down — they open the table, then realise their chat can play along — so the
 * link is refreshed every time their socket comes up rather than only when they join.
 *
 * The Twitch picture only fills an empty face: a player already wearing their Discord
 * one, or one they picked themselves, keeps it.
 */
internal class LinkTwitchHandler {

    fun handle(state: GameState, command: GameCommand.LinkTwitch): CommandResult {
        val player = state.playerOf(command.playerId)
            ?: return CommandResult.rejected(GameError.UNKNOWN_PLAYER)
        val linked = player.linkedTo(command)
        if (linked == player) return CommandResult.accepted(state)
        return CommandResult.accepted(
            state.copy(players = state.players.map { if (it.id == player.id) linked else it })
        )
    }

    private fun Player.linkedTo(command: GameCommand.LinkTwitch): Player = copy(
        twitchLogin = command.login,
        avatar = if (avatar.pictureUrl == null && command.pictureUrl != null) {
            avatar.copy(pictureUrl = command.pictureUrl)
        } else {
            avatar
        },
    )
}
