package fr.ftnl.cardgame.domain.engine

import fr.ftnl.cardgame.domain.card.CardId
import fr.ftnl.cardgame.domain.card.CardPool
import fr.ftnl.cardgame.domain.game.ChatVoteTally
import fr.ftnl.cardgame.domain.game.GameSettings
import fr.ftnl.cardgame.domain.game.SubmissionId
import fr.ftnl.cardgame.domain.player.Player
import fr.ftnl.cardgame.domain.player.PlayerId

/** Every legal intent a player, or the server scheduler, can express on a game. */
sealed interface GameCommand {

    data class Join(val player: Player) : GameCommand

    data class Leave(val playerId: PlayerId) : GameCommand

    data class SetConnected(val playerId: PlayerId, val connected: Boolean) : GameCommand

    /**
     * Issued by the scheduler once the disconnect grace delay has elapsed: frees the seat
     * of [playerId] unless the match is under way, or they came back in the meantime.
     */
    data class DropIfAway(val playerId: PlayerId) : GameCommand

    data class Kick(val by: PlayerId, val playerId: PlayerId) : GameCommand

    /**
     * Attaches the Twitch account of a player who signed in, possibly long after taking
     * their seat. A null [login] unlinks them, which is what a sign out amounts to.
     */
    data class LinkTwitch(
        val playerId: PlayerId,
        val login: String?,
        val pictureUrl: String? = null,
    ) : GameCommand

    data class UpdateSettings(val by: PlayerId, val settings: GameSettings) : GameCommand

    data class SetCardPool(val by: PlayerId, val pool: CardPool) : GameCommand

    data class Start(val by: PlayerId) : GameCommand

    /** [fills] carries, per played card, the words to drop into that card's own holes. */
    data class PlayCards(
        val playerId: PlayerId,
        val cardIds: List<CardId>,
        val fills: List<List<String>> = emptyList(),
    ) : GameCommand

    data class WriteAnswers(val playerId: PlayerId, val texts: List<String>) : GameCommand

    /** A vote in [fr.ftnl.cardgame.domain.game.SelectionMode.VOTE], the czar pick otherwise. */
    data class Choose(val playerId: PlayerId, val submissionId: SubmissionId) : GameCommand

    /**
     * The live tally read from the Twitch chats, pushed by the server while the vote is
     * open: per answer, how many viewers picked it and the first faces behind them. It
     * replaces the previous snapshot, so a lost frame corrects itself on the next one.
     */
    data class SetChatVotes(val tallies: Map<SubmissionId, ChatVoteTally>) : GameCommand

    /** Issued by the scheduler when the submission timer runs out. */
    data object CloseSubmissions : GameCommand

    /** Issued by the scheduler when the selection timer runs out. */
    data object CloseSelection : GameCommand

    /** Started by the host, or by the scheduler when [by] is null. */
    data class NextRound(val by: PlayerId?) : GameCommand

    /** Sends a finished game back to its lobby so the host can deal a fresh match. */
    data class ReturnToLobby(val by: PlayerId) : GameCommand
}
