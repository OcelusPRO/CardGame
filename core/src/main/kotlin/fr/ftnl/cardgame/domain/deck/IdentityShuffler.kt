package fr.ftnl.cardgame.domain.deck

/** Keeps the insertion order untouched, making game scenarios fully predictable. */
object IdentityShuffler : Shuffler {
    override fun <T> shuffle(items: List<T>): List<T> = items
}
