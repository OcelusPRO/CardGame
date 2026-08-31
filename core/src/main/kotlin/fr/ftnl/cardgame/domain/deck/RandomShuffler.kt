package fr.ftnl.cardgame.domain.deck

import kotlin.random.Random

/** Default shuffler; accepts a seeded [Random] so a whole game can be replayed in tests. */
class RandomShuffler(private val random: Random = Random.Default) : Shuffler {
    override fun <T> shuffle(items: List<T>): List<T> = items.shuffled(random)
}
