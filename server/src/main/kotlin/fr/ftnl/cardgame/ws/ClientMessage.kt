package fr.ftnl.cardgame.ws

import fr.ftnl.cardgame.api.dto.AvatarInput
import fr.ftnl.cardgame.api.dto.DeckInput
import fr.ftnl.cardgame.api.dto.GameSettingsInput
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Everything the browser may send on the game socket. The `type` field selects the shape. */
@Serializable
sealed interface ClientMessage {

    @Serializable
    @SerialName("play")
    data class PlayCards(
        val cardIds: List<String>,
        /** Per played card, the words the player typed into that card's own holes. */
        val fills: List<List<String>> = emptyList(),
    ) : ClientMessage

    @Serializable
    @SerialName("write")
    data class WriteAnswers(val texts: List<String>) : ClientMessage

    @Serializable
    @SerialName("choose")
    data class Choose(val answerId: Int) : ClientMessage

    @Serializable
    @SerialName("settings")
    data class UpdateSettings(val settings: GameSettingsInput) : ClientMessage

    @Serializable
    @SerialName("deck")
    data class UpdateDeck(val deck: DeckInput) : ClientMessage

    @Serializable
    @SerialName("kick")
    data class Kick(val playerId: String) : ClientMessage

    /** Trades the seat for the stream page; a host names who takes the crown. */
    @Serializable
    @SerialName("step_aside")
    data class StepAside(val heir: String? = null) : ClientMessage

    /** One more player sitting around a shared device. */
    @Serializable
    @SerialName("add_seat")
    data class AddSeat(val nickname: String, val avatar: AvatarInput) : ClientMessage

    /**
     * Hands a shared device to [playerId]: from now on the socket sees their hand and
     * acts for them. Null turns the screen back to the whole table, where nobody's cards
     * show and the device acts as the host.
     */
    @Serializable
    @SerialName("seat")
    data class Seat(val playerId: String? = null) : ClientMessage

    @Serializable
    @SerialName("start")
    data object Start : ClientMessage

    @Serializable
    @SerialName("next")
    data object NextRound : ClientMessage

    @Serializable
    @SerialName("lobby")
    data object ReturnToLobby : ClientMessage

    @Serializable
    @SerialName("leave")
    data object Leave : ClientMessage

    @Serializable
    @SerialName("ping")
    data object Ping : ClientMessage
}
