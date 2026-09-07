package fr.ftnl.cardgame.api.dto

import kotlinx.serialization.Serializable

/**
 * The whole game as one viewer sees it. The client renders this and nothing else, which
 * keeps the "who may see what" decision in a single place on the server.
 */
@Serializable
data class GameView(
    val code: String,
    val phase: String,
    val hostId: String,
    val settings: GameSettingsView,
    val players: List<PlayerView>,
    val you: SelfView,
    val round: RoundView? = null,
    val deck: DeckSummary,
    val deadlineMillis: Long? = null,
    val serverTimeMillis: Long,
    /** The Twitch channels whose chat votes on this table; empty when nobody's does. */
    val chatChannels: List<String> = emptyList(),
    /** What the chats wrote into the paquet. Empty for everyone but the host. */
    val chatCardLog: ChatCardLogView = ChatCardLogView(),
    /**
     * True when this snapshot was projected for the stream page rather than for a seat:
     * [you] is empty, and nothing on screen may offer an action. The client could infer it
     * from the empty `you.id`, but a page that hides a streamer's hand should not hang on
     * an inference.
     */
    val spectator: Boolean = false,
)
