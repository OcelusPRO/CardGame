package fr.ftnl.cardgame.domain.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AvatarTest {

    private val top = AvatarPart("head-3", "#AABBCC")
    private val bottom = AvatarPart("body-1", "#112233")

    @Test
    fun `accepts a Discord picture served by the Discord CDN`() {
        val avatar = Avatar(top, bottom, "https://cdn.discordapp.com/avatars/1/2.png")

        assertEquals("https://cdn.discordapp.com/avatars/1/2.png", avatar.pictureUrl)
    }

    @Test
    fun `accepts a Twitch picture served by the Twitch CDN`() {
        val url = "https://static-cdn.jtvnw.net/jtv_user_pictures/1-profile_image-300x300.png"

        assertEquals(url, Avatar(top, bottom, url).pictureUrl)
    }

    @Test
    fun `refuses a picture hosted anywhere else`() {
        assertFailsWith<IllegalArgumentException> { Avatar(top, bottom, "https://evil.example/pic.png") }
    }

    @Test
    fun `refuses an unknown colour format`() {
        assertFailsWith<IllegalArgumentException> { AvatarPart("head-1", "red") }
    }

    @Test
    fun `refuses a style id with unexpected characters`() {
        assertFailsWith<IllegalArgumentException> { AvatarPart("Head 1", "#ffffff") }
    }
}
