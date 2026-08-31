package fr.ftnl.cardgame.domain.player

import kotlinx.serialization.Serializable

/** The display name a player picks when joining, normalised and length checked. */
@Serializable
@JvmInline
value class Nickname private constructor(val value: String) {

    override fun toString(): String = value

    companion object {
        const val MIN_LENGTH = 2
        const val MAX_LENGTH = 20

        /** Collapses every run of whitespace into a single space, then validates the length. */
        fun of(raw: String): Nickname {
            val cleaned = collapseWhitespace(raw)
            require(cleaned.length >= MIN_LENGTH) { "A nickname needs at least $MIN_LENGTH characters" }
            require(cleaned.length <= MAX_LENGTH) { "A nickname cannot exceed $MAX_LENGTH characters" }
            return Nickname(cleaned)
        }

        /** Same as [of] but returns `null` instead of throwing on user supplied input. */
        fun ofOrNull(raw: String): Nickname? = runCatching { of(raw) }.getOrNull()

        private fun collapseWhitespace(raw: String): String = raw
            .map { if (it.isWhitespace()) ' ' else it }
            .joinToString(separator = "")
            .split(' ')
            .filter { it.isNotEmpty() }
            .joinToString(separator = " ")
    }
}
