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
)
