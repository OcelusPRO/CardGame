package fr.ftnl.cardgame.domain.card

import kotlinx.serialization.Serializable

/**
 * A card holding the funny answer played against a [SituationCard]. Its text may itself
 * carry a run of underscores: a hole the player fills in with their own words when they
 * play the card, on top of whatever the situation asks for.
 */
@Serializable
data class PunchlineCard(
    val id: CardId,
    val text: String,
    val origin: CardOrigin = CardOrigin.OFFICIAL,
) {
    init {
        require(text.isNotBlank()) { "A punchline text cannot be blank" }
    }

    /** How many holes the player must fill on this card, zero for a plain one. */
    val blankCount: Int get() = PLACEHOLDER.findAll(text).count()

    /**
     * The card text with [fills] dropped into its holes, in order. A plain card, or a
     * missing fill, is left untouched.
     */
    fun fill(fills: List<String>): String {
        if (blankCount == 0) return text
        var index = 0
        return PLACEHOLDER.replace(text) { match ->
            fills.getOrNull(index++)?.trim()?.ifBlank { null }?.trimEnd('.') ?: match.value
        }
    }

    private companion object {
        val PLACEHOLDER = Regex("_{2,}")
    }
}
