package fr.ftnl.cardgame.domain.game

import kotlinx.serialization.Serializable

/** What a viewer has to do before their idea is allowed onto the table. */
enum class ChatCardAccess {
    /** Nobody proposes anything; the chat is only ever an audience. */
    OFF,

    /** Any message in the chat counts. Made for a small, friendly room. */
    EVERYONE,

    /**
     * Only a channel point redemption counts. The game creates the reward on the
     * streamer's own channel — one per pile, named and priced here — and follows what is
     * redeemed on it, so nothing has to be typed in the chat and a refused card gives
     * the points back.
     */
    CHANNEL_POINTS,

    /** Only a cheer counts, and only from [ChatCardSettings.minBits] up. */
    BITS,
}

/**
 * One channel point reward, as the streamer's viewers will see it.
 *
 * It comes one of two ways, and [id] is which. Blank, the game **creates** the reward when
 * the mode opens and takes it down when the table breaks up — hence a title and a price
 * set here rather than on Twitch beforehand. Filled, the host pointed at a reward already
 * standing on their channel: the game adopts it, follows it, and leaves it exactly where
 * it found it.
 *
 * A reward can only be adopted if the game created it in the first place. Twitch reserves
 * managing a reward to the application whose client id made it, so one built in the
 * dashboard cannot be followed, renamed or removed by anybody but its streamer — which is
 * why the lobby asks them to delete it themselves before rebuilding it here.
 */
@Serializable
data class ChatCardReward(
    /** A reward already on the channel, when the host picked one. Blank means: make one. */
    val id: String = "",
    val title: String = "",
    val cost: Int = DEFAULT_COST,
) {
    init {
        require(title.length <= MAX_TITLE) { "reward title must be at most $MAX_TITLE characters" }
        require(cost in MIN_COST..MAX_COST) { "reward cost must be within $MIN_COST..$MAX_COST" }
    }

    /** True when the game is to put this reward up itself, rather than adopt one. */
    val isNew: Boolean get() = id.isBlank()

    companion object {
        /** Twitch's own ceiling on a reward title; a longer one is simply refused. */
        const val MAX_TITLE = 45
        const val MIN_COST = 1
        const val MAX_COST = 1_000_000
        const val DEFAULT_COST = 500

        val SITUATION = ChatCardReward(title = "Écrire une situation", cost = DEFAULT_COST)
        val PUNCHLINE = ChatCardReward(title = "Écrire une réponse", cost = DEFAULT_COST)
    }
}

/**
 * Lets the viewers write cards, not just judge them.
 *
 * A situation typed by the chat lands in the pile the table draws from, and a punchline in
 * the pile the hands are dealt from — so a viewer's idea comes back a few minutes later in
 * somebody's hand, which is the entire point. Off by default: an open chat writing straight
 * onto the table is a decision the host makes, not one they discover.
 *
 * The gate is Twitch's own, and each way in arrives by its own road. A free line and a
 * cheer ride on the IRC message itself, so nothing is asked of anybody. Channel points
 * take the other road: the game creates the reward on the channel and reads the
 * redemptions through EventSub, which is what lets it refuse a card and hand the points
 * back rather than keep them for a card nobody ever saw.
 */
@Serializable
data class ChatCardSettings(
    val access: ChatCardAccess = ChatCardAccess.OFF,
    /** Whether `!situation` is open, and whether `!réponse` is. */
    val situations: Boolean = true,
    val punchlines: Boolean = true,
    /** The cheer a proposal costs, in [ChatCardAccess.BITS]. */
    val minBits: Int = DEFAULT_MIN_BITS,
    /** The rewards created on the channel, in [ChatCardAccess.CHANNEL_POINTS]. */
    val situationReward: ChatCardReward = ChatCardReward.SITUATION,
    val punchlineReward: ChatCardReward = ChatCardReward.PUNCHLINE,
) {
    init {
        require(minBits in 0..MAX_MIN_BITS) { "minBits must be within 0..$MAX_MIN_BITS" }
    }

    val enabled: Boolean get() = access != ChatCardAccess.OFF && (situations || punchlines)

    /** True when Twitch has to have charged the viewer for the line to count. */
    val paid: Boolean get() = access == ChatCardAccess.CHANNEL_POINTS || access == ChatCardAccess.BITS

    /**
     * Whether the cards come in through the chat. Everything does but the channel points,
     * which come through the rewards the game owns — a redemption is not a chat line, and
     * reading the room would only pick up people talking about it.
     */
    val readsChat: Boolean get() = enabled && access != ChatCardAccess.CHANNEL_POINTS

    /** Whether the game has rewards to create on the host's channel, and to follow. */
    val usesRewards: Boolean get() = enabled && access == ChatCardAccess.CHANNEL_POINTS

    companion object {
        const val DEFAULT_MIN_BITS = 100
        const val MAX_MIN_BITS = 100_000

        /**
         * How many cards one chat may push into a single game.
         *
         * A safety valve, and nothing more: a snapshot is written on every command, so the
         * pile a chat can grow needs *a* ceiling or a bot could grow it without end. It is
         * set far above what any stream writes on purpose — nobody is meant to meet it, and
         * it is therefore never spelled out to the viewers or to the host.
         */
        const val MAX_CARDS_PER_GAME = 5_000
    }
}
