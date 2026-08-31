package fr.ftnl.cardgame.domain.game

import kotlinx.serialization.Serializable

/**
 * The short code shared with friends to join a game. Built on an alphabet without
 * look-alike characters, so a code survives being dictated out loud or read off a
 * screen: no O or 0, no I or 1, and no Q either, which reads as an O at small sizes.
 */
@Serializable
@JvmInline
value class GameCode private constructor(val value: String) {

    override fun toString(): String = value

    companion object {
        const val LENGTH = 5
        const val ALPHABET = "ABCDEFGHJKMNPRSTUVWXYZ23456789"

        /** Normalises [raw] (trim + uppercase) then validates it. */
        fun of(raw: String): GameCode {
            val normalised = raw.trim().uppercase()
            require(normalised.length == LENGTH) { "A game code holds exactly $LENGTH characters" }
            require(normalised.all { it in ALPHABET }) { "Game code $normalised uses forbidden characters" }
            return GameCode(normalised)
        }

        /** Same as [of] but returns `null` instead of throwing on user supplied input. */
        fun ofOrNull(raw: String): GameCode? = runCatching { of(raw) }.getOrNull()
    }
}
