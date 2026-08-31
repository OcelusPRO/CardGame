package fr.ftnl.cardgame.seed

import fr.ftnl.cardgame.domain.game.GameClock
import fr.ftnl.cardgame.support.FakeCardPackRepository
import fr.ftnl.cardgame.support.FakePunchlineCardRepository
import fr.ftnl.cardgame.support.FakeSituationCardRepository
import fr.ftnl.cardgame.catalog.CatalogPunchline
import fr.ftnl.cardgame.domain.card.CardId
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DevDeckSeederTest {

    private val packs = FakeCardPackRepository()
    private val situations = FakeSituationCardRepository()
    private val punchlines = FakePunchlineCardRepository()
    private val seeder = DevDeckSeeder(packs, situations, punchlines, GameClock { 0 })

    @Test
    fun `an empty catalogue receives the bundled demo deck`() = runBlocking {
        assertTrue(seeder.seed())

        assertEquals(1, packs.all().size)
        assertTrue(situations.count() > 20)
        assertTrue(punchlines.count() > 60)
    }

    @Test
    fun `a catalogue that already holds cards is left alone`() = runBlocking {
        punchlines.save(CatalogPunchline(CardId("p1"), "base", "déjà là"))

        assertFalse(seeder.seed())
        assertEquals(1, punchlines.count())
    }

    @Test
    fun `every seeded situation is playable`() = runBlocking {
        seeder.seed()

        assertTrue(situations.all().all { it.text.blankCount >= 1 })
        assertTrue(situations.all().all { it.enabled })
    }
}
