package fr.ftnl.cardgame.domain.card

import kotlinx.serialization.Serializable

/** A card describing the situation of a round, the one punchlines answer to. */
@Serializable
data class SituationCard(
    val id: CardId,
    val text: SituationText,
    val origin: CardOrigin = CardOrigin.OFFICIAL,
) {
    /** How many punchlines this situation expects from each player. */
    val blankCount: Int get() = text.blankCount
}
