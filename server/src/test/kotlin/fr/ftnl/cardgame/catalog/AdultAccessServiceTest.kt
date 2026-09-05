package fr.ftnl.cardgame.catalog

import fr.ftnl.cardgame.api.dto.AdultAccessInput
import fr.ftnl.cardgame.auth.Account
import fr.ftnl.cardgame.auth.AccountLookup
import fr.ftnl.cardgame.auth.AccountProvider
import fr.ftnl.cardgame.auth.UnknownAccountException
import fr.ftnl.cardgame.domain.game.GameClock
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * What an administrator types, and what actually gets stored: always an account id, even
 * when a channel name was typed, and always a readable label.
 */
class AdultAccessServiceTest {

    private val clock = GameClock { 1_000 }

    private class FakeAccess : AdultPackAccessRepository {
        val entries = mutableListOf<AdultPackAccess>()
        override suspend fun all() = entries.toList()
        override suspend fun add(entry: AdultPackAccess) {
            entries.removeAll { it.provider == entry.provider && it.accountId == entry.accountId }
            entries += entry
        }
        override suspend fun remove(provider: AccountProvider, accountId: String) =
            entries.removeAll { it.provider == provider && it.accountId == accountId }
        override suspend fun contains(provider: AccountProvider, accountId: String) =
            entries.any { it.provider == provider && it.accountId == accountId }
    }

    /** A directory that knows one streamer, by id as well as by channel name. */
    private val kameto = AccountLookup { provider, query ->
        Account(
            provider = AccountProvider.TWITCH,
            id = "44322889",
            displayName = "Kameto",
            login = "kameto",
        ).takeIf {
            provider == AccountProvider.TWITCH && query.lowercase() in setOf("kameto", "44322889")
        }
    }

    private fun service(lookup: AccountLookup = AccountLookup.NONE, access: FakeAccess = FakeAccess()) =
        AdultAccessService(access, clock, lookup)

    @Test
    fun `a channel name is stored as the id behind it`() = runBlocking {
        val added = service(kameto).add(AdultAccessInput(provider = "TWITCH", accountId = "kameto"))

        assertEquals("44322889", added.accountId)
        assertEquals("TWITCH", added.provider)
    }

    @Test
    fun `the pseudo fills itself in when no label was typed`() = runBlocking {
        val added = service(kameto).add(AdultAccessInput(provider = "TWITCH", accountId = "44322889"))

        assertEquals("Kameto", added.label)
    }

    @Test
    fun `a label the administrator typed is never overwritten`() = runBlocking {
        val added = service(kameto)
            .add(AdultAccessInput(provider = "TWITCH", accountId = "kameto", label = "Le patron"))

        assertEquals("Le patron", added.label)
    }

    @Test
    fun `an at sign pasted along with the channel name is ignored`() = runBlocking {
        val added = service(kameto).add(AdultAccessInput(provider = "TWITCH", accountId = " @Kameto "))

        assertEquals("44322889", added.accountId)
    }

    @Test
    fun `a channel nobody can find is refused`() {
        assertFailsWith<UnknownAccountException> {
            runBlocking { service(kameto).add(AdultAccessInput(provider = "TWITCH", accountId = "fantome")) }
        }
    }

    @Test
    fun `a plain id still works with no directory at all`() = runBlocking {
        val added = service().add(AdultAccessInput(accountId = "100000000000000042", label = "Alex"))

        assertEquals("100000000000000042", added.accountId)
        assertEquals("DISCORD", added.provider)
        assertEquals("Alex", added.label)
    }

    @Test
    fun `an empty identifier is refused outright`() {
        assertFailsWith<IllegalArgumentException> {
            runBlocking { service().add(AdultAccessInput(accountId = "  ")) }
        }
    }

    @Test
    fun `adding the same account twice keeps the day it was first cleared`() = runBlocking {
        val access = FakeAccess()
        val service = service(kameto, access)
        service.add(AdultAccessInput(provider = "TWITCH", accountId = "kameto"))
        service.add(AdultAccessInput(provider = "TWITCH", accountId = "44322889", label = "Kam"))

        assertEquals(1, access.entries.size)
        assertEquals(1_000, access.entries.single().addedAtMillis)
    }
}
