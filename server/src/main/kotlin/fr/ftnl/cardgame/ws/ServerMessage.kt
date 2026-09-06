package fr.ftnl.cardgame.ws

import fr.ftnl.cardgame.api.dto.ChatAnswerVotesView
import fr.ftnl.cardgame.api.dto.GameView
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Everything the server pushes on the game socket. */
@Serializable
sealed interface ServerMessage {

    @Serializable
    @SerialName("state")
    data class State(val game: GameView) : ServerMessage

    /**
     * The running Twitch chat tally, on its own frame. It travels apart from [State]
     * because it moves once a second and carries nothing else: sending a whole snapshot
     * for it would re-encode the deck and hit Redis at that same cadence.
     */
    @Serializable
    @SerialName("chat_votes")
    data class ChatVotes(val answers: List<ChatAnswerVotesView>) : ServerMessage

    /** A refused command; [code] is a [fr.ftnl.cardgame.domain.engine.GameError] name. */
    @Serializable
    @SerialName("error")
    data class Failure(val code: String) : ServerMessage

    @Serializable
    @SerialName("pong")
    data object Pong : ServerMessage
}
