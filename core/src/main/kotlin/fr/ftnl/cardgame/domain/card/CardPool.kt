package fr.ftnl.cardgame.domain.card

import kotlinx.serialization.Serializable

/**
 * The whole set of cards a game may draw from, resolved before the game starts
 * by merging the official decks with the custom cards of the host.
 */
@Serializable
data class CardPool(
    val situations: List<SituationCard>,
    val punchlines: List<PunchlineCard>,
) {
    /** Merges two pools, dropping cards whose id is already present. */
    operator fun plus(other: CardPool): CardPool = CardPool(
        situations = mergeById(situations, other.situations) { it.id },
        punchlines = mergeById(punchlines, other.punchlines) { it.id },
    )

    private fun <T> mergeById(first: List<T>, second: List<T>, id: (T) -> CardId): List<T> =
        (first + second).distinctBy(id)

    companion object {
        val EMPTY = CardPool(emptyList(), emptyList())
    }
}
