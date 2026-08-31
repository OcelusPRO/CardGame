package fr.ftnl.cardgame.domain.deck

/** Result of taking cards off a [DrawPile]: what was drawn, and the pile left behind. */
data class Draw<T>(
    val cards: List<T>,
    val pile: DrawPile<T>,
)
