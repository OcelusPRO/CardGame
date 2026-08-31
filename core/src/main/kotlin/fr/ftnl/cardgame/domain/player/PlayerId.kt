package fr.ftnl.cardgame.domain.player

import kotlinx.serialization.Serializable

/** Identifier of a player, valid only for the lifetime of their game session. */
@Serializable
@JvmInline
value class PlayerId(val value: String) {
    init {
        require(value.isNotBlank()) { "A player id cannot be blank" }
    }

    override fun toString(): String = value
}
