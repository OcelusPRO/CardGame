package fr.ftnl.cardgame.domain.game

/** Wall clock used in production. */
object SystemGameClock : GameClock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
