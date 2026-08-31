package fr.ftnl.cardgame.domain.player

import kotlinx.serialization.Serializable

/** A participant of a game, identified for the duration of the session only. */
@Serializable
data class Player(
    val id: PlayerId,
    val nickname: Nickname,
    val avatar: Avatar,
    val connected: Boolean = true,
)
