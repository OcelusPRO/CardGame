package fr.ftnl.cardgame.domain.player

import kotlinx.serialization.Serializable

/**
 * One half of an avatar: the style picked in the frontend catalogue plus its colour.
 * The domain only guards the format, the artwork itself lives in the web app.
 */
@Serializable
data class AvatarPart(
    val styleId: String,
    val color: String,
) {
    init {
        require(STYLE_ID.matches(styleId)) { "Invalid avatar style id: $styleId" }
        require(COLOR.matches(color)) { "Invalid avatar colour: $color" }
    }

    private companion object {
        val STYLE_ID = Regex("[a-z0-9-]{1,32}")
        val COLOR = Regex("#[0-9a-fA-F]{6}")
    }
}
