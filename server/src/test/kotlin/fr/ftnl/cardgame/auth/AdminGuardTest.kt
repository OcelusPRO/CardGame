package fr.ftnl.cardgame.auth

import fr.ftnl.cardgame.config.AdminConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AdminGuardTest {

    private val guard = AdminGuard(AdminConfig(setOf("111", "222")))

    @Test
    fun `an allowlisted account becomes an administrator`() {
        val user = DiscordUser(id = "111", username = "root", globalName = "Root")

        assertTrue(guard.isAdmin(user))
        assertEquals("Root", guard.sessionFor(user)?.username)
    }

    @Test
    fun `any other account gets nothing`() {
        val user = DiscordUser(id = "999", username = "curieux")

        assertFalse(guard.isAdmin(user))
        assertNull(guard.sessionFor(user))
    }

    @Test
    fun `the avatar url points at the Discord CDN`() {
        val user = DiscordUser(id = "111", username = "root", avatar = "abc")

        assertEquals("https://cdn.discordapp.com/avatars/111/abc?size=128", user.avatarUrl)
    }

    @Test
    fun `the avatar url carries no extension so Discord picks the format`() {
        val animated = DiscordUser(id = "111", username = "root", avatar = "a_1234")

        assertFalse(animated.avatarUrl.orEmpty().contains(".png"))
    }

    @Test
    fun `an account with the default picture has no avatar url`() {
        assertNull(DiscordUser(id = "111", username = "root").avatarUrl)
    }

    @Test
    fun `the display name falls back on the login`() {
        assertEquals("root", DiscordUser(id = "111", username = "root").displayName)
    }
}
