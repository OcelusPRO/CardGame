package fr.ftnl.cardgame.game

import fr.ftnl.cardgame.api.dto.GameTicket
import fr.ftnl.cardgame.domain.engine.GameError

/** Result of trying to take a seat at an existing table. */
sealed interface JoinOutcome {

    data class Joined(val ticket: GameTicket) : JoinOutcome

    data class Refused(val error: GameError) : JoinOutcome

    data object NotFound : JoinOutcome
}
