package fr.ftnl.cardgame.domain.card

import kotlinx.serialization.Serializable

/** Stable identifier of a card, unique inside a single card pool. */
@Serializable
@JvmInline
value class CardId(val value: String) {
    init {
        require(value.isNotBlank()) { "A card id cannot be blank" }
    }

    override fun toString(): String = value
}
