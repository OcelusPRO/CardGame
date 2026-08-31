package fr.ftnl.cardgame.domain.game

import fr.ftnl.cardgame.domain.card.PunchlineCard
import fr.ftnl.cardgame.domain.player.PlayerId
import kotlinx.serialization.Serializable

/**
 * What a player answered this round: cards from their hand, or texts they wrote
 * themselves in free mode. Exactly one of the two lists is filled.
 *
 * [cardFills] holds, per played card, the words the player typed into that card's own
 * holes. It is either empty (no card had a hole) or lined up one-to-one with [cards].
 */
@Serializable
data class Submission(
    val playerId: PlayerId,
    val cards: List<PunchlineCard> = emptyList(),
    val texts: List<String> = emptyList(),
    val cardFills: List<List<String>> = emptyList(),
) {
    init {
        require(cards.isEmpty() != texts.isEmpty()) { "A submission holds either cards or texts" }
        require(texts.none { it.isBlank() }) { "A written answer cannot be blank" }
        require(cardFills.isEmpty() || cardFills.size == cards.size) {
            "Card fills, when present, line up with the played cards"
        }
        require(cardFills.flatten().none { it.isBlank() }) { "A card fill cannot be blank" }
    }

    /** The answers as plain text, holes filled in, whatever the mode they were produced in. */
    val answers: List<String>
        get() = if (cards.isNotEmpty()) {
            cards.mapIndexed { index, card -> card.fill(cardFills.getOrElse(index) { emptyList() }) }
        } else {
            texts
        }

    /** How many answers this submission carries, to match the situation blanks. */
    val size: Int get() = maxOf(cards.size, texts.size)
}
