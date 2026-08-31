package fr.ftnl.cardgame.domain.game

/** The step a game currently sits on; every command is only legal in some of them. */
enum class GamePhase {
    /** Waiting room: players join, the host tunes the settings. */
    LOBBY,

    /** Players pick their punchline cards, or write them in free mode. */
    SUBMITTING,

    /** Everyone votes, or the card czar picks the best answer. */
    SELECTING,

    /** Answers and points of the round are revealed. */
    ROUND_RESULT,

    /** Somebody reached the target score, or the last round has been played. */
    FINISHED,
}
