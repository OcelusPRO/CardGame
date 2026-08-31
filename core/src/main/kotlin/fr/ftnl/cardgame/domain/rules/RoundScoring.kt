package fr.ftnl.cardgame.domain.rules

import fr.ftnl.cardgame.domain.game.GameSettings
import fr.ftnl.cardgame.domain.game.Round
import fr.ftnl.cardgame.domain.game.RoundOutcome

/** Turns the choices made during a round into the points they are worth. */
fun interface RoundScoring {
    fun score(round: Round, settings: GameSettings): RoundOutcome
}
