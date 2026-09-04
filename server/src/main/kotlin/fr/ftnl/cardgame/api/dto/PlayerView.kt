package fr.ftnl.cardgame.api.dto

import kotlinx.serialization.Serializable

/** A player as seen by everybody else: never their hand, only their progress. */
@Serializable
data class PlayerView(
    val id: String,
    val nickname: String,
    val avatar: AvatarView,
    val connected: Boolean,
    val score: Int,
    val isHost: Boolean,
    val isCzar: Boolean,
    val hasAnswered: Boolean,
    val hasVoted: Boolean,
    /** Their Twitch channel, when they signed in with it; the chat the table may read. */
    val twitchLogin: String? = null,
)
