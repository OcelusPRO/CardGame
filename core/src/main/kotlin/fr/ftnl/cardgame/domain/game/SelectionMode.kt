package fr.ftnl.cardgame.domain.game

/** How the best answer of a round gets designated. */
enum class SelectionMode {
    /** Every player votes; a vote is worth points and a majority earns a bonus. */
    VOTE,

    /** A rotating card czar picks the winner and does not play that round. */
    CZAR,

    /**
     * The Twitch chats judge, and them alone: everybody at the table answers, nobody at
     * the table votes, and the viewers pick the winner by typing an answer number.
     */
    CHAT,
}
