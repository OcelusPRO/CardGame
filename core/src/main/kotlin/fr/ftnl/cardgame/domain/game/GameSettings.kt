package fr.ftnl.cardgame.domain.game

import kotlinx.serialization.Serializable

/** Everything the host can tune before, and only before, starting the game. */
@Serializable
data class GameSettings(
    /** Who judges: everybody, a rotating czar, or the Twitch chats. */
    val selectionMode: SelectionMode = SelectionMode.VOTE,
    /** How they judge: every answer at once, or two at a time. Free of [selectionMode]. */
    val selectionFormat: SelectionFormat = SelectionFormat.ALL_AT_ONCE,
    val answerMode: AnswerMode = AnswerMode.CARDS,
    val scoring: ScoringSettings = ScoringSettings(),
    /** How many situation cards the game will play through. */
    val rounds: Int = 8,
    val handSize: Int = 10,
    val submitSeconds: Int = 90,
    val selectSeconds: Int = 60,
    /**
     * How long a single duel stays open in [SelectionFormat.DUELS]. Two answers are read
     * in a breath, so it is a fraction of [selectSeconds]: a round is many of these.
     */
    val duelSeconds: Int = 20,
    val resultSeconds: Int = 10,
    val minPlayers: Int = MIN_PLAYERS,
    val maxPlayers: Int = 12,
    /** Lets a player vote for their own answer, for tables that find that funnier. */
    val allowSelfVote: Boolean = false,
    /**
     * In [SelectionMode.CZAR], lets the rotating czar also submit an answer for the round.
     * They still make the pick, and simply cannot choose their own answer. Ignored in
     * [SelectionMode.VOTE], where everybody answers already.
     */
    val czarAnswers: Boolean = false,
    /**
     * In [SelectionMode.CHAT], reads the chat of every other player signed in with Twitch
     * as well, so a table of streamers plays in front of all of their communities at once.
     */
    val twitchGuestChats: Boolean = false,
    /** Whether — and at what price — the viewers may write cards into this game. */
    val chatCards: ChatCardSettings = ChatCardSettings(),
) {
    init {
        require(rounds in MIN_ROUNDS..MAX_ROUNDS) { "rounds must be within $MIN_ROUNDS..$MAX_ROUNDS" }
        require(handSize in MIN_HAND_SIZE..MAX_HAND_SIZE) { "handSize must be within $MIN_HAND_SIZE..$MAX_HAND_SIZE" }
        require(submitSeconds in MIN_TIMER..MAX_TIMER) { "submitSeconds must be within $MIN_TIMER..$MAX_TIMER" }
        require(selectSeconds in MIN_TIMER..MAX_TIMER) { "selectSeconds must be within $MIN_TIMER..$MAX_TIMER" }
        require(duelSeconds in MIN_DUEL_TIMER..MAX_DUEL_TIMER) { "duelSeconds must be within $MIN_DUEL_TIMER..$MAX_DUEL_TIMER" }
        require(resultSeconds in MIN_RESULT_PAUSE..MAX_RESULT_PAUSE) { "resultSeconds must be within $MIN_RESULT_PAUSE..$MAX_RESULT_PAUSE" }
        require(maxPlayers in MIN_PLAYERS..MAX_PLAYERS) { "maxPlayers must be within $MIN_PLAYERS..$MAX_PLAYERS" }
        require(minPlayers in MIN_PLAYERS..maxPlayers) { "minPlayers must be within $MIN_PLAYERS..maxPlayers" }
    }

    /** In free mode no punchline card is ever dealt. */
    val dealsCards: Boolean get() = answerMode == AnswerMode.CARDS

    /** True when the round is judged duel by duel rather than in one go. */
    val runsBracket: Boolean get() = selectionFormat == SelectionFormat.DUELS

    /** How long the step that is about to open should last, in seconds. */
    val judgingSeconds: Int get() = if (runsBracket) duelSeconds else selectSeconds

    companion object {
        const val MIN_PLAYERS = 2
        const val MAX_PLAYERS = 24
        const val MIN_ROUNDS = 1
        const val MAX_ROUNDS = 50
        const val MIN_HAND_SIZE = 4
        const val MAX_HAND_SIZE = 15
        const val MIN_RESULT_PAUSE = 3
        const val MAX_RESULT_PAUSE = 60
        const val MIN_TIMER = 15
        const val MAX_TIMER = 300
        const val MIN_DUEL_TIMER = 5
        const val MAX_DUEL_TIMER = 120
    }
}
