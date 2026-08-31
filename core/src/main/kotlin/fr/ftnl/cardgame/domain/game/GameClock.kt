package fr.ftnl.cardgame.domain.game

/**
 * Time source of the domain, expressed in epoch milliseconds so a game snapshot
 * stays trivially serialisable. Tests inject a fixed clock.
 */
fun interface GameClock {
    fun nowMillis(): Long
}
