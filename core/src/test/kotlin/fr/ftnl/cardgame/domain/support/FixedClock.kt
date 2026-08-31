package fr.ftnl.cardgame.domain.support

import fr.ftnl.cardgame.domain.game.GameClock

/** A clock the tests move by hand, so deadlines are exact values instead of ranges. */
class FixedClock(private var millis: Long = 1_000_000) : GameClock {
    override fun nowMillis(): Long = millis

    fun advance(seconds: Long) {
        millis += seconds * 1000
    }
}
