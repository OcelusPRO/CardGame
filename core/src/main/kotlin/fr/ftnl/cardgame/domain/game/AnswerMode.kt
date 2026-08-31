package fr.ftnl.cardgame.domain.game

/** Where the answers of a round come from. */
enum class AnswerMode {
    /** Players play punchline cards drawn from their hand. */
    CARDS,

    /** "Sans limites": no punchline deck, players write their own answer. */
    FREE_TEXT,
}
