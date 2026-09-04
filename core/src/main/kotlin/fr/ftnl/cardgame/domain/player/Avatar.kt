package fr.ftnl.cardgame.domain.player

import kotlinx.serialization.Serializable

/**
 * A player avatar built from two independently customisable halves.
 * When the player signed in — with Discord or with Twitch — [pictureUrl] is drawn over
 * the top half.
 */
@Serializable
data class Avatar(
    val top: AvatarPart,
    val bottom: AvatarPart,
    val pictureUrl: String? = null,
) {
    init {
        require(pictureUrl == null || CDNS.any { pictureUrl.startsWith(it) }) {
            "A profile picture must be served by the Discord or the Twitch CDN"
        }
    }

    private companion object {
        val CDNS = listOf("https://cdn.discordapp.com/", "https://static-cdn.jtvnw.net/")
    }
}
