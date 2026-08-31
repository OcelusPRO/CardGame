package fr.ftnl.cardgame.domain.deck

import kotlinx.serialization.Serializable

/**
 * An immutable pile of cards. Once the face down stack runs out, the discard stack is
 * shuffled back in, so a small deck can feed a long game without ever going empty.
 */
@Serializable
data class DrawPile<T>(
    val available: List<T>,
    val discarded: List<T> = emptyList(),
) {
    val size: Int get() = available.size + discarded.size

    /** Takes up to [count] cards, recycling the discard stack when the pile runs dry. */
    fun draw(count: Int, shuffler: Shuffler): Draw<T> {
        require(count >= 0) { "Cannot draw a negative amount of cards" }
        val taken = available.take(count)
        val missing = count - taken.size
        if (missing == 0) return Draw(taken, copy(available = available.drop(count)))
        return recycle(shuffler).drawAfterRecycle(taken, missing, shuffler)
    }

    /** Puts [cards] aside; they come back into play at the next recycle. */
    fun discard(cards: List<T>): DrawPile<T> = copy(discarded = discarded + cards)

    private fun recycle(shuffler: Shuffler): DrawPile<T> =
        DrawPile(available = shuffler.shuffle(discarded), discarded = emptyList())

    private fun drawAfterRecycle(taken: List<T>, missing: Int, shuffler: Shuffler): Draw<T> {
        if (available.isEmpty()) return Draw(taken, this)
        val second = draw(missing, shuffler)
        return Draw(taken + second.cards, second.pile)
    }

    companion object {
        /** Builds a shuffled pile out of [cards]. */
        fun <T> shuffled(cards: List<T>, shuffler: Shuffler): DrawPile<T> =
            DrawPile(shuffler.shuffle(cards))
    }
}
