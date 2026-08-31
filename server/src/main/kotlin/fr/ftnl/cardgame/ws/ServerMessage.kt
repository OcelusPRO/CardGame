package fr.ftnl.cardgame.ws

import fr.ftnl.cardgame.api.dto.GameView
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Everything the server pushes on the game socket. */
@Serializable
sealed interface ServerMessage {

    @Serializable
    @SerialName("state")
    data class State(val game: GameView) : ServerMessage

    /** A refused command; [code] is a [fr.ftnl.cardgame.domain.engine.GameError] name. */
    @Serializable
    @SerialName("error")
    data class Failure(val code: String) : ServerMessage

    @Serializable
    @SerialName("pong")
    data object Pong : ServerMessage
}
