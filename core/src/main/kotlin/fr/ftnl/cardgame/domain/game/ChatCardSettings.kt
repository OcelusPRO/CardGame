package fr.ftnl.cardgame.domain.game

import kotlinx.serialization.Serializable

/** What a viewer has to do before their idea is allowed onto the table. */
enum class ChatCardAccess {
    /** Nobody proposes anything; the chat is only ever an audience. */
    OFF,

    /** Any message in the chat counts. Made for a small, friendly room. */
    EVERYONE,

    /**
     * Only a channel point redemption counts — the kind that asks the viewer to type
     * something. Twitch marks the message with the reward that paid for it, so the price
     * is set on the channel, by the streamer, and this side only checks it was paid.
     */
    CHANNEL_POINTS,

    /** Only a cheer counts, and only from [ChatCardSettings.minBits] up. */
    BITS,
}

/**
 * Lets the viewers write cards, not just judge them.
 *
 * A situation typed by the chat lands in the pile the table draws from, and a punchline in
 * the pile the hands are dealt from — so a viewer's idea comes back a few minutes later in
 * somebody's hand, which is the entire point. Off by default: an open chat writing straight
 * onto the table is a decision the host makes, not one they discover.
 *
 * The gate is deliberately Twitch's own. Channel points and bits are already the two
 * things a chat understands as "this costs something", both are already priced by the
 * streamer, and both arrive on the very same IRC line as the message — so nothing here
 * needs a token, a webhook, or a viewer to grant anything.
 */
@Serializable
data class ChatCardSettings(
    val access: ChatCardAccess = ChatCardAccess.OFF,
    /** Whether `!situation` is open, and whether `!réponse` is. */
    val situations: Boolean = true,
    val punchlines: Boolean = true,
    /** The cheer a proposal costs, in [ChatCardAccess.BITS]. */
    val minBits: Int = DEFAULT_MIN_BITS,
    /**
     * Restricts [ChatCardAccess.CHANNEL_POINTS] to one reward. Blank accepts any of them,
     * which is what a streamer with a single "propose une carte" reward wants.
     */
    val rewardId: String = "",
) {
    init {
        require(minBits in 0..MAX_MIN_BITS) { "minBits must be within 0..$MAX_MIN_BITS" }
    }

    val enabled: Boolean get() = access != ChatCardAccess.OFF && (situations || punchlines)

    /** True when Twitch has to have charged the viewer for the line to count. */
    val paid: Boolean get() = access == ChatCardAccess.CHANNEL_POINTS || access == ChatCardAccess.BITS

    companion object {
        const val DEFAULT_MIN_BITS = 100
        const val MAX_MIN_BITS = 100_000

        /**
         * How many cards one chat may push into a single game. A snapshot is written on
         * every command, so the pile a chat can grow has to have a ceiling — and a table
         * that has taken two hundred ideas from its viewers has had its fill anyway.
         */
        const val MAX_CARDS_PER_GAME = 200
    }
}
