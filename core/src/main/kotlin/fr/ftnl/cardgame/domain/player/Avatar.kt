package fr.ftnl.cardgame.domain.player

import kotlinx.serialization.Serializable

/**
 * A player avatar built from two independently customisable halves.
 * When the player signed in with Discord, [discordAvatarUrl] is drawn over the top half.
 */
@Serializable
data class Avatar(
    val top: AvatarPart,
    val bottom: AvatarPart,
    val discordAvatarUrl: String? = null,
) {
    init {
        require(discordAvatarUrl == null || discordAvatarUrl.startsWith(DISCORD_CDN)) {
            "A Discord avatar must be served by the Discord CDN"
        }
    }

    private companion object {
        const val DISCORD_CDN = "https://cdn.discordapp.com/"
    }
}
