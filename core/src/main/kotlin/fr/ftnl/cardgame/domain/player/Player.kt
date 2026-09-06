package fr.ftnl.cardgame.domain.player

import kotlinx.serialization.Serializable

/**
 * A participant of a game, identified for the duration of the session only.
 *
 * [twitchLogin] is the channel name of the player when they signed in with Twitch. It is
 * what lets the table read their chat, and it is public information: it is the very name
 * anybody types in a browser to watch them.
 *
 * [twitchId] is the numeric account behind that name. A channel can be renamed and a name
 * can be reused, so it is the id — never the name — that the Twitch extension is matched
 * against when a viewer sends something from the panel under the stream.
 */
@Serializable
data class Player(
    val id: PlayerId,
    val nickname: Nickname,
    val avatar: Avatar,
    val connected: Boolean = true,
    val twitchLogin: String? = null,
    val twitchId: String? = null,
)
