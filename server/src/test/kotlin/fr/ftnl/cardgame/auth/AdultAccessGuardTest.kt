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
    }

    private val clock = GameClock { NOW }

    private class FakeAccess(private val ids: MutableSet<String> = mutableSetOf()) : AdultPackAccessRepository {
        override suspend fun all() = ids.map { AdultPackAccess(it, "", 0) }
        override suspend fun add(entry: AdultPackAccess) { ids += entry.discordId }
        override suspend fun remove(discordId: String) = ids.remove(discordId)
        override suspend fun contains(discordId: String) = discordId in ids
    }

    /** The snowflake id an account would carry if it had been created at [millis]. */
    private fun idCreatedAt(millis: Long): String = ((millis - DISCORD_EPOCH) shl 22).toString()

    private fun guard(
        access: AdultPackAccessRepository = FakeAccess(),
        admins: Set<String> = emptySet(),
        minAccountAgeDays: Int = 1095,
    ) = AdultAccessGuard(access, AdminConfig(admins), clock, AdultAccessConfig(minAccountAgeDays))

    @Test
    fun `an account older than the threshold is trusted without the allowlist`() = runBlocking {
        val old = idCreatedAt(NOW - 1200 * DAY)
        assertTrue(guard().allows(old))
    }

    @Test
    fun `a younger account is not trusted on its age alone`() = runBlocking {
        val young = idCreatedAt(NOW - 400 * DAY)
        val guard = guard()
        assertFalse(guard.allows(young))

        // ...but the allowlist still lets it through.
        val access = FakeAccess()
        access.add(AdultPackAccess(young, "", 0))
        assertTrue(guard(access = access).allows(young))
    }

    @Test
    fun `the age heuristic is off when the threshold is zero`() = runBlocking {
        val old = idCreatedAt(NOW - 3000 * DAY)
        assertFalse(guard(minAccountAgeDays = 0).allows(old))
    }

    @Test
    fun `a non snowflake id falls back to the allowlist and the admins`() = runBlocking {
        assertFalse(guard().allows("not-a-snowflake"))
        assertTrue(guard(admins = setOf("root-1")).allows("root-1"))
    }

    @Test
    fun `an anonymous host is refused`() = runBlocking {
        assertFalse(guard().allows(null))
        assertFalse(guard().allows(""))
    }
}
