package fr.ftnl.cardgame.api.dto

import kotlinx.serialization.Serializable

/** Everything private to the viewer: their hand and what the game expects from them. */
@Serializable
data class SelfView(
    val id: String,
    val hand: List<PunchlineCardView> = emptyList(),
    val isHost: Boolean = false,
    val isCzar: Boolean = false,
    val mustAnswer: Boolean = false,
    val mustVote: Boolean = false,
)
