package fr.ftnl.cardgame.api.dto

import kotlinx.serialization.Serializable

/**
 * What the extension panel needs to draw itself: whether there is a table to write to,
 * what it accepts, and what it costs here.
 *
 * No game code, no nicknames, no scores. A viewer is looking at a stream that already
 * shows them the table; what the panel adds is a way in, not a second scoreboard.
 */
@Serializable
data class ExtensionStateView(
    /** False when the streamer has no table running, or has not opened it to the chat. */
    val open: Boolean = false,
    val situations: Boolean = false,
    val punchlines: Boolean = false,
    /** `EVERYONE`, `CHANNEL_POINTS` or `BITS`; absent when nothing is open. */
    val access: String = "OFF",
    /** The bits products the panel may offer, cheapest first. Empty outside the bits mode. */
    val products: List<ExtensionProductView> = emptyList(),
    /**
     * The channel point rewards to send the viewer to, in the points mode. A redemption
     * never reaches an extension, so all the panel can do is name them.
     */
    val rewards: List<ExtensionRewardView> = emptyList(),
    /** How many cards the chat has already put on this table, and the ceiling. */
    val written: Int = 0,
    val limit: Int = 0,
)

/** One channel point reward standing on the channel, as the panel needs to name it. */
@Serializable
data class ExtensionRewardView(val title: String, val cost: Int)

/** One bits product, as the panel needs it to draw a button. */
@Serializable
data class ExtensionProductView(val sku: String, val bits: Int)

/** A card a viewer is sending from the panel. */
@Serializable
data class ExtensionCardRequest(
    /** `SITUATION` or `PUNCHLINE`. */
    val kind: String,
    val text: String,
    /**
     * The signed receipt handed back by `Twitch.ext.bits.useBits`. Required in the bits
     * mode and ignored otherwise — the amount is read from the configured product, never
     * from anything the panel says it paid.
     */
    val receipt: String? = null,
)

/** What became of it. */
@Serializable
data class ExtensionCardResponse(val accepted: Boolean, val message: String)
