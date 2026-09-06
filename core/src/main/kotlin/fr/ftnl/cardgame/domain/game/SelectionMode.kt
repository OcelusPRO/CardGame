package fr.ftnl.cardgame.domain.game

/**
 * **Who** designates the best answer of a round.
 *
 * How they do it — everything at once, or two answers at a time — is [SelectionFormat],
 * and the two are chosen independently.
 */
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
