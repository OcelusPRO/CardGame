package fr.ftnl.cardgame.domain.engine.handler

import fr.ftnl.cardgame.domain.engine.CommandResult
import fr.ftnl.cardgame.domain.engine.GameCommand
import fr.ftnl.cardgame.domain.engine.GameError
import fr.ftnl.cardgame.domain.game.ChatVoteTally
import fr.ftnl.cardgame.domain.game.GamePhase
import fr.ftnl.cardgame.domain.game.GameState

/**
 * Publishes the tally read from the Twitch chats onto the round, so every screen at the
 * table shows the same live count and the same faces. Anything the snapshot cannot
 * justify is dropped here rather than trusted: an answer number the viewers made up, or
 * more faces than the table is meant to show.
 */
internal class ChatVoteHandler {

    fun handle(state: GameState, command: GameCommand.SetChatVotes): CommandResult {
        val round = state.round ?: return CommandResult.rejected(GameError.WRONG_PHASE)
        if (state.phase != GamePhase.SELECTING) return CommandResult.rejected(GameError.WRONG_PHASE)
        if (state.chatChannels.isEmpty()) return CommandResult.rejected(GameError.CHAT_VOTE_CLOSED)
        val tallies = command.tallies
            .filterKeys { round.authorOf(it) != null }
            .mapValues { (_, tally) -> tally.trimmed() }
            .filterValues { it.count > 0 }
        return CommandResult.accepted(state.copy(round = round.withChatVotes(tallies)))
    }

    private fun ChatVoteTally.trimmed(): ChatVoteTally =
        if (voters.size <= ChatVoteTally.MAX_FACES) this
        else copy(voters = voters.take(ChatVoteTally.MAX_FACES))
}
