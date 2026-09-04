package fr.ftnl.cardgame.domain.engine.handler

import fr.ftnl.cardgame.domain.engine.CommandResult
import fr.ftnl.cardgame.domain.engine.GameCommand
import fr.ftnl.cardgame.domain.engine.GameError
import fr.ftnl.cardgame.domain.game.GamePhase
import fr.ftnl.cardgame.domain.game.GameState

/**
 * Publishes the tally read from the Twitch chats onto the round, so every screen at the
 * table shows the same live count. Anything the snapshot cannot justify is dropped here
 * rather than trusted: a channel nobody at the table streams on, or an answer number the
 * viewers made up.
 */
internal class ChatVoteHandler {

    fun handle(state: GameState, command: GameCommand.SetChatVotes): CommandResult {
        val round = state.round ?: return CommandResult.rejected(GameError.WRONG_PHASE)
        if (state.phase != GamePhase.SELECTING) return CommandResult.rejected(GameError.WRONG_PHASE)
        val channels = state.chatChannels
        if (channels.isEmpty()) return CommandResult.rejected(GameError.CHAT_VOTE_CLOSED)
        val tallies = command.tallies
            .filterKeys { it in channels }
            .mapValues { (_, tally) -> tally.filterKeys { round.authorOf(it) != null && it.index >= 0 } }
            .filterValues { it.isNotEmpty() }
        return CommandResult.accepted(state.copy(round = round.withChatVotes(tallies)))
    }
}
