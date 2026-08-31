package fr.ftnl.cardgame.domain.game

import kotlinx.serialization.Serializable

/** Everything the host can tune before, and only before, starting the game. */
@Serializable
data class GameSettings(
    val selectionMode: SelectionMode = SelectionMode.VOTE,
    val answerMode: AnswerMode = AnswerMode.CARDS,
    val scoring: ScoringSettings = ScoringSettings(),
    /** How many situation cards the game will play through. */
    val rounds: Int = 8,
    val handSize: Int = 10,
    val submitSeconds: Int = 90,
    val selectSeconds: Int = 60,
    val resultSeconds: Int = 10,
    val minPlayers: Int = 3,
    val maxPlayers: Int = 12,
    /** Lets a player vote for their own answer, for tables that find that funnier. */
    val allowSelfVote: Boolean = false,
    /**
     * In [SelectionMode.CZAR], lets the rotating czar also submit an answer for the round.
     * They still make the pick, and simply cannot choose their own answer. Ignored in
     * [SelectionMode.VOTE], where everybody answers already.
     */
    val czarAnswers: Boolean = false,
) {
    init {
        require(rounds in MIN_ROUNDS..MAX_ROUNDS) { "rounds must be within $MIN_ROUNDS..$MAX_ROUNDS" }
        require(handSize in MIN_HAND_SIZE..MAX_HAND_SIZE) { "handSize must be within $MIN_HAND_SIZE..$MAX_HAND_SIZE" }
        require(submitSeconds in MIN_TIMER..MAX_TIMER) { "submitSeconds must be within $MIN_TIMER..$MAX_TIMER" }
        require(selectSeconds in MIN_TIMER..MAX_TIMER) { "selectSeconds must be within $MIN_TIMER..$MAX_TIMER" }
        require(resultSeconds in MIN_RESULT_PAUSE..MAX_RESULT_PAUSE) { "resultSeconds must be within $MIN_RESULT_PAUSE..$MAX_RESULT_PAUSE" }
        require(maxPlayers in MIN_PLAYERS..MAX_PLAYERS) { "maxPlayers must be within $MIN_PLAYERS..$MAX_PLAYERS" }
        require(minPlayers in MIN_PLAYERS..maxPlayers) { "minPlayers must be within $MIN_PLAYERS..maxPlayers" }
    }

    /** In free mode no punchline card is ever dealt. */
    val dealsCards: Boolean get() = answerMode == AnswerMode.CARDS

    companion object {
        const val MIN_PLAYERS = 2
        const val MAX_PLAYERS = 20
        const val MIN_ROUNDS = 1
        const val MAX_ROUNDS = 50
        const val MIN_HAND_SIZE = 4
        const val MAX_HAND_SIZE = 15
        const val MIN_RESULT_PAUSE = 3
        const val MAX_RESULT_PAUSE = 60
        const val MIN_TIMER = 15
        const val MAX_TIMER = 300
    }
}
