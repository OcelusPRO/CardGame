package fr.ftnl.cardgame.domain.game

import fr.ftnl.cardgame.domain.card.PunchlineCard
import fr.ftnl.cardgame.domain.card.SituationCard
import fr.ftnl.cardgame.domain.deck.DrawPile
import fr.ftnl.cardgame.domain.player.Player
import fr.ftnl.cardgame.domain.player.PlayerId
import kotlinx.serialization.Serializable

/**
 * The complete snapshot of a game. It is immutable: every command produces a new
 * instance, which is what gets stored in Redis and broadcast to the players.
 */
@Serializable
data class GameState(
    val code: GameCode,
    val hostId: PlayerId,
    val players: List<Player> = emptyList(),
    val settings: GameSettings = GameSettings(),
    val phase: GamePhase = GamePhase.LOBBY,
    val situations: DrawPile<SituationCard> = DrawPile(emptyList()),
    val punchlines: DrawPile<PunchlineCard> = DrawPile(emptyList()),
    val hands: Map<PlayerId, List<PunchlineCard>> = emptyMap(),
    val scoreboard: Scoreboard = Scoreboard(),
    val round: Round? = null,
    val phaseDeadlineMillis: Long? = null,
    val createdAtMillis: Long = 0,
    /** What the Twitch chats have written into this game, kept for the host to read. */
    val chatCardLog: ChatCardLog = ChatCardLog.EMPTY,
) {
    fun playerOf(playerId: PlayerId): Player? = players.firstOrNull { it.id == playerId }

    fun contains(playerId: PlayerId): Boolean = playerOf(playerId) != null

    fun isHost(playerId: PlayerId): Boolean = hostId == playerId

    fun handOf(playerId: PlayerId): List<PunchlineCard> = hands[playerId].orEmpty()

    /**
     * Whether this player has already cast the vote the step in front of them expects.
     * A ladder scopes that to the duel on the table, so the same player votes again a
     * duel later; judged all at once, the question is asked once per round.
     */
    fun hasVoted(playerId: PlayerId): Boolean {
        val round = round ?: return false
        if (!settings.runsBracket) return round.hasVoted(playerId)
        return round.bracket?.current?.hasVoted(playerId) ?: true
    }

    /** The answer this player picked in the step in front of them, if they picked one. */
    fun voteOf(playerId: PlayerId): SubmissionId? {
        val round = round ?: return null
        return if (settings.runsBracket) round.bracket?.current?.votes?.get(playerId)
        else round.votes[playerId]
    }

    /** Players still online; a disconnected player keeps their score and their seat. */
    val connectedPlayers: List<Player> get() = players.filter { it.connected }

    /**
     * The Twitch channels judging this table, in the order they sat down: the host first,
     * then the other streamers when the host asked for their chats too. Empty in every
     * mode but [SelectionMode.CHAT], where the players decide among themselves.
     */
    val chatChannels: List<String>
        get() = if (settings.selectionMode == SelectionMode.CHAT) twitchChannels else emptyList()

    /**
     * Every Twitch channel attached to this table, whatever the table uses them for: the
     * host first, then the other streamers when the host asked for their chats too.
     *
     * [chatChannels] is the subset that judges; the viewers writing cards read from this
     * one instead, since a chat can be given a pen without being given a vote.
     */
    val twitchChannels: List<String>
        get() {
            val guests = if (settings.twitchGuestChats) players.filter { it.id != hostId } else emptyList()
            return (listOfNotNull(playerOf(hostId)?.twitchLogin) + guests.mapNotNull { it.twitchLogin })
                .distinct()
        }

    /**
     * The channels whose chat may write cards right now; empty unless the host opened it.
     *
     * The channel point mode is absent on purpose: there the cards arrive through the
     * rewards the game creates, and reading the room would only pick up the people
     * talking about the reward rather than redeeming it.
     */
    val cardChannels: List<String>
        get() = if (settings.chatCards.readsChat) twitchChannels else emptyList()

    /** The channel the game owns its channel point rewards on: the host's, and only theirs. */
    val rewardChannelId: String?
        get() = if (settings.chatCards.usesRewards) playerOf(hostId)?.twitchId else null

    /**
     * How many cards the chats have written into this game, both piles together.
     *
     * Read off the log rather than the piles, so a card that has since been drawn, played
     * or replaced still counts against the ceiling: the limit is on what a chat may write,
     * not on what happens to be waiting to be dealt.
     */
    val chatCardCount: Int get() = chatCardLog.size

    /** True when the chat is voting right now, which is what puts the numbers on screen. */
    val chatVoteOpen: Boolean get() = phase == GamePhase.SELECTING && chatChannels.isNotEmpty()

    /**
     * The answers the viewers are choosing between, in the order they appear on screen.
     *
     * A chat types a **position**, not an answer id — `2` is the second card in front of
     * them. Judging a whole round that is every answer; judging a duel it is exactly two,
     * which is the entire appeal of running a big chat on a ladder: `1` or `2`, and no
     * viewer has to scan twelve cards to find the number they meant.
     */
    val chatChoices: List<SubmissionId>
        get() {
            val round = round ?: return emptyList()
            if (!settings.runsBracket) return round.revealed.map { (id, _) -> id }
            val duel = round.bracket?.current ?: return emptyList()
            return listOfNotNull(duel.left, duel.right)
        }

    /**
     * Which count a chat tally belongs to. A ladder restarts the count at every duel, so
     * the round number alone would let the voices of a settled duel decide the next one.
     */
    val chatVoteScope: ChatVoteScope?
        get() {
            val round = round ?: return null
            val bracket = round.bracket
            return ChatVoteScope(
                round = round.number,
                tier = bracket?.tier ?: 0,
                duel = bracket?.currentNumber ?: 0,
            )
        }

    /** True when the rotating czar also submits an answer, see [GameSettings.czarAnswers]. */
    val czarAnswers: Boolean
        get() = settings.selectionMode == SelectionMode.CZAR && settings.czarAnswers

    /**
     * Whether [voter] may pick their own answer.
     *
     * Ordinarily that is the table-wide [GameSettings.allowSelfVote]. A czar who also
     * answers is the exception, and not an optional one: they are the only voter of the
     * round, so barring them from their own card means the card they wrote could never
     * win — they would be dealing themselves out of the game they just joined.
     */
    fun allowsSelfVote(voter: PlayerId): Boolean =
        if (settings.selectionMode == SelectionMode.CZAR) czarAnswers && round?.czarId == voter
        else settings.allowSelfVote

    /** Players expected to answer this round, which excludes the card czar unless [czarAnswers]. */
    val answeringPlayers: List<Player>
        get() = if (czarAnswers) connectedPlayers
        else connectedPlayers.filter { it.id != round?.czarId }

    /** How many answers each player must provide, driven by the situation holes. */
    val expectedAnswers: Int get() = round?.situation?.blankCount ?: 1

    val isOver: Boolean get() = phase == GamePhase.FINISHED

    /**
     * True while a match is actually being played. Outside of it a dropped player is
     * removed from the table straight away; during it their seat is kept so they can
     * reconnect, and it is only freed once the game ends.
     */
    val isMidGame: Boolean
        get() = phase == GamePhase.SUBMITTING ||
            phase == GamePhase.SELECTING ||
            phase == GamePhase.ROUND_RESULT
}
