package fr.ftnl.cardgame.auth

import fr.ftnl.cardgame.config.AdminConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AdminGuardTest {

    private val guard = AdminGuard(
        AdminConfig(discordIds = setOf("111", "222"), twitchIds = setOf("333")),
    )

    @Test
    fun `an allowlisted Discord account becomes an administrator`() {
        val user = DiscordUser(id = "111", username = "root", globalName = "Root")

        assertTrue(guard.isAdmin(user.account()))
        assertEquals("Root", guard.sessionFor(user.account())?.username)
    }

    @Test
    fun `an allowlisted Twitch account becomes one too`() {
        val user = TwitchUser(id = "333", login = "root", displayName = "Root")

        val session = guard.sessionFor(user.account())

        assertEquals("TWITCH", session?.provider)
        assertEquals("333", session?.accountId)
    }

    @Test
    fun `an id listed for one provider grants nothing to the other`() {
        val twitchTwin = TwitchUser(id = "111", login = "sosie")
        val discordTwin = DiscordUser(id = "333", username = "sosie")

        assertFalse(guard.isAdmin(twitchTwin.account()))
        assertFalse(guard.isAdmin(discordTwin.account()))
    }

    @Test
    fun `any other account gets nothing`() {
        val user = DiscordUser(id = "999", username = "curieux")

        assertFalse(guard.isAdmin(user.account()))
        assertNull(guard.sessionFor(user.account()))
    }

    @Test
    fun `a Twitch account carries the day it was opened, which its id never tells`() {
        val user = TwitchUser(id = "1", login = "kameto", createdAt = "2016-03-02T10:15:30Z")

        assertEquals(1_456_913_730_000L, user.account().createdAtMillis)
    }

    @Test
    fun `a creation date Twitch never sent leaves the age unknown`() {
        assertNull(TwitchUser(id = "1", login = "kameto").account().createdAtMillis)
        assertNull(TwitchUser(id = "1", login = "kameto", createdAt = "hier").account().createdAtMillis)
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
