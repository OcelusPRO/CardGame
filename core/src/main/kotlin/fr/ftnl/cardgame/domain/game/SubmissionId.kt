package fr.ftnl.cardgame.domain.game

import kotlinx.serialization.Serializable

/**
 * Anonymous handle of an answer during the selection step. It is the position in the
 * shuffled reveal order, so it never leaks who played what.
 */
@Serializable
@JvmInline
value class SubmissionId(val index: Int) {
    init {
        require(index >= 0) { "A submission index cannot be negative" }
    }

    override fun toString(): String = "s$index"
}
