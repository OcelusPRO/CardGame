package fr.ftnl.cardgame.domain.deck

/** Strategy used every time the game needs to randomise an ordered list. */
interface Shuffler {
    fun <T> shuffle(items: List<T>): List<T>
}
