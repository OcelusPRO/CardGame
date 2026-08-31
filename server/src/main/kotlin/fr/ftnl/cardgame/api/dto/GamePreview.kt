package fr.ftnl.cardgame.api.dto

import kotlinx.serialization.Serializable

/**
 * What the game URL shows before anybody sits down. [youArePlaying] is what lets a single
 * address serve both the table and the form to join it.
 */
@Serializable
data class GamePreview(
    val code: String,
    val phase: String,
    val hostNickname: String,
    val playerCount: Int,
    val maxPlayers: Int,
    val canJoin: Boolean,
    val youArePlaying: Boolean,
)
