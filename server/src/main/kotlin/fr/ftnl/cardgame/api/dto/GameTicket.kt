package fr.ftnl.cardgame.api.dto

import kotlinx.serialization.Serializable

/**
 * Handed back when a player creates or joins a game: the code to share, the link the
 * QR code encodes, and the identity the WebSocket will use.
 */
@Serializable
data class GameTicket(
    val code: String,
    val playerId: String,
    val joinUrl: String,
    val isHost: Boolean,
)
