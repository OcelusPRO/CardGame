package fr.ftnl.cardgame.domain.player

import kotlinx.serialization.Serializable

/**
 * A participant of a game, identified for the duration of the session only.
 *
 * [twitchLogin] is the channel name of the player when they signed in with Twitch. It is
 * what lets the table read their chat, and it is public information: it is the very name
 * anybody types in a browser to watch them.
 */
@Serializable
data class Player(
    val id: PlayerId,
    val nickname: Nickname,
    val avatar: Avatar,
    val connected: Boolean = true,
    val twitchLogin: String? = null,
)
