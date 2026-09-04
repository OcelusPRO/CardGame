package fr.ftnl.cardgame.auth

import fr.ftnl.cardgame.catalog.AdultPackAccess
import fr.ftnl.cardgame.catalog.AdultPackAccessRepository
import fr.ftnl.cardgame.config.AdminConfig
import fr.ftnl.cardgame.config.AdultAccessConfig
import fr.ftnl.cardgame.domain.game.GameClock
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdultAccessGuardTest {

    private companion object {
        const val DISCORD_EPOCH = 1_420_070_400_000L
        const val DAY = 24L * 60 * 60 * 1000
        const val NOW = 1_767_225_600_000L // 2026-01-01
        const val THREE_YEARS = 1095
    }

    private val clock = GameClock { NOW }

    private class FakeAccess(
        private val entries: MutableSet<Pair<AccountProvider, String>> = mutableSetOf(),
    ) : AdultPackAccessRepository {
        override suspend fun all() = entries.map { AdultPackAccess(it.first, it.second) }
        override suspend fun add(entry: AdultPackAccess) {
            entries += entry.provider to entry.accountId
        }
        override suspend fun remove(provider: AccountProvider, accountId: String) =
            entries.remove(provider to accountId)
        override suspend fun contains(provider: AccountProvider, accountId: String) =
            (provider to accountId) in entries
    }

    /** The snowflake id an account would carry if it had been created at [millis]. */
    private fun idCreatedAt(millis: Long): String = ((millis - DISCORD_EPOCH) shl 22).toString()

    private fun discord(millis: Long) = PlayerSession(playerId = "p1", discordId = idCreatedAt(millis))

    private fun twitch(id: String = "42", createdAtMillis: Long? = null) =
        PlayerSession(playerId = "p1", twitchId = id, twitchCreatedAtMillis = createdAtMillis)

    private fun guard(
        access: AdultPackAccessRepository = FakeAccess(),
        admins: AdminConfig = AdminConfig(emptySet()),
        minAccountAgeDays: Int = THREE_YEARS,
    ) = AdultAccessGuard(access, admins, clock, AdultAccessConfig(minAccountAgeDays))

    @Test
    fun `a Discord account older than the threshold is trusted without the allowlist`() = runBlocking {
        assertTrue(guard().allows(discord(NOW - 1200 * DAY)))
    }

    @Test
    fun `a Twitch account past three years is trusted just the same`() = runBlocking {
        assertTrue(guard().allows(twitch(createdAtMillis = NOW - 1200 * DAY)))
    }

    @Test
    fun `a Twitch account opened last year is not`() = runBlocking {
        assertFalse(guard().allows(twitch(createdAtMillis = NOW - 300 * DAY)))
    }

    @Test
    fun `a Twitch account whose age we never learned falls back on the lists`() = runBlocking {
        assertFalse(guard().allows(twitch(id = "77")))

        val access = FakeAccess()
        access.add(AdultPackAccess(AccountProvider.TWITCH, "77"))
        assertTrue(guard(access = access).allows(twitch(id = "77")))
    }

    @Test
    fun `an allowlisted Twitch id does not clear the Discord account carrying the same number`() =
        runBlocking {
            val access = FakeAccess()
            access.add(AdultPackAccess(AccountProvider.TWITCH, "12345"))
            // Age rule off: this is about the lists, and "12345" happens to be a valid
            // — and very old — Discord snowflake.
            val guard = guard(access = access, minAccountAgeDays = 0)

            assertFalse(guard.allows(PlayerSession(playerId = "p1", discordId = "12345")))
            assertTrue(guard.allows(twitch(id = "12345")))
        }

    @Test
    fun `an allowlisted Twitch administrator is cleared too`() = runBlocking {
        val admins = AdminConfig(discordIds = emptySet(), twitchIds = setOf("777"))

        assertTrue(guard(admins = admins).allows(twitch(id = "777")))
    }

    @Test
    fun `a younger Discord account is not trusted on its age alone`() = runBlocking {
        val young = discord(NOW - 400 * DAY)
        assertFalse(guard().allows(young))

        val access = FakeAccess()
        access.add(AdultPackAccess(AccountProvider.DISCORD, young.discordId!!))
        assertTrue(guard(access = access).allows(young))
    }

    @Test
    fun `the age heuristic is off when the threshold is zero`() = runBlocking {
        assertFalse(guard(minAccountAgeDays = 0).allows(discord(NOW - 3000 * DAY)))
        assertFalse(guard(minAccountAgeDays = 0).allows(twitch(createdAtMillis = NOW - 3000 * DAY)))
    }

    @Test
    fun `either sign in can carry the clearance`() = runBlocking {
        val access = FakeAccess()
        access.add(AdultPackAccess(AccountProvider.TWITCH, "88"))
        val both = PlayerSession(playerId = "p1", discordId = "not-a-snowflake", twitchId = "88")

        assertTrue(guard(access = access).allows(both))
    }

    @Test
    fun `an anonymous host is refused`() = runBlocking {
        assertFalse(guard().allows(null))
        assertFalse(guard().allows(PlayerSession(playerId = "p1")))
    }
}
