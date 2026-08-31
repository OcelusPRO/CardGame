package fr.ftnl.cardgame.stats

import java.time.LocalDate
import java.time.ZoneOffset

/** Names the day a statistics row belongs to; injected so tests can pin a date. */
fun interface DayProvider {
    fun today(): String

    companion object {
        val UTC = DayProvider { LocalDate.now(ZoneOffset.UTC).toString() }
    }
}
