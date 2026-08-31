package fr.ftnl.cardgame.domain.card

/** Tells where a card comes from, which drives whether usage statistics are recorded. */
enum class CardOrigin {
    /** Curated card stored in the database and shared by every game. */
    OFFICIAL,

    /** Card written by a player, alive only for the duration of their session. */
    CUSTOM,
}
