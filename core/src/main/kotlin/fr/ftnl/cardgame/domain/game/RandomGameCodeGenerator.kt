package fr.ftnl.cardgame.domain.game

import kotlin.random.Random

/** Draws each character uniformly from [GameCode.ALPHABET]. */
class RandomGameCodeGenerator(private val random: Random = Random.Default) : GameCodeGenerator {
    override fun generate(): GameCode =
        GameCode.of((1..GameCode.LENGTH).map { GameCode.ALPHABET.random(random) }.joinToString(""))
}
