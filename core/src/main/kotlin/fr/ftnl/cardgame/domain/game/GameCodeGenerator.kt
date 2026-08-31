package fr.ftnl.cardgame.domain.game

/** Produces the codes handed out when a game is created. */
fun interface GameCodeGenerator {
    fun generate(): GameCode
}
