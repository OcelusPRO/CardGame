package fr.ftnl.cardgame.domain.game

/**
 * **How** the answers are judged, which is a separate question from **who** judges them —
 * see [SelectionMode].
 *
 * The two combine freely: a card czar can rule on duels, a chat of four thousand can pick
 * between two answers instead of twelve, and a table can vote the ordinary way. Whoever
 * holds the vote, they hold it the same way; only the number of answers in front of them
 * at any moment changes.
 */
enum class SelectionFormat {
    /** Every answer on the table at once, one vote, the usual way. */
    ALL_AT_ONCE,

    /**
     * Answers face off two by two until one is left, and a point goes to each duel won.
     *
     * Made for a crowded table or a crowded chat: reading a dozen answers to pick one is a
     * chore that ends in nobody reading anything, and two at a time is a reflex.
     */
    DUELS,
}
