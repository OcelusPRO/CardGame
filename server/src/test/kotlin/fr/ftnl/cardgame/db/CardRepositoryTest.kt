package fr.ftnl.cardgame.db

import fr.ftnl.cardgame.catalog.CardPack
import fr.ftnl.cardgame.catalog.CatalogPunchline
import fr.ftnl.cardgame.catalog.CatalogSituation
import fr.ftnl.cardgame.catalog.ExposedCardPackRepository
import fr.ftnl.cardgame.catalog.ExposedPunchlineCardRepository
import fr.ftnl.cardgame.catalog.ExposedSituationCardRepository
import fr.ftnl.cardgame.domain.card.CardId
import fr.ftnl.cardgame.domain.card.SituationText
import fr.ftnl.cardgame.support.TestDatabase
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Exercises the real SQL against an in-memory PostgreSQL compatible database. */
class CardRepositoryTest {

    private val packs = ExposedCardPackRepository()
    private val situations = ExposedSituationCardRepository()
    private val punchlines = ExposedPunchlineCardRepository()

    @BeforeTest
    fun setUp() = runBlocking {
        TestDatabase.connect()
        packs.save(CardPack("base", "Base", "Le pack de départ"))
    }

    @Test
    fun `a situation is stored and read back`() = runBlocking {
        situations.save(CatalogSituation(CardId("s1"), "base", SituationText("____ et ____ ?")))

        val stored = situations.find(CardId("s1"))

        assertEquals("____ et ____ ?", stored?.text?.raw)
        assertEquals(2, stored?.text?.blankCount)
    }

    @Test
    fun `saving twice updates instead of duplicating`() = runBlocking {
        situations.save(CatalogSituation(CardId("s1"), "base", SituationText("avant ____")))
        situations.save(CatalogSituation(CardId("s1"), "base", SituationText("après ____")))

        assertEquals(1, situations.count())
        assertEquals("après ____", situations.find(CardId("s1"))?.text?.raw)
    }

    @Test
    fun `disabled cards are left out of a game deck`() = runBlocking {
        punchlines.save(CatalogPunchline(CardId("p1"), "base", "visible"))
        punchlines.save(CatalogPunchline(CardId("p2"), "base", "masquée", enabled = false))

        val playable = punchlines.enabledIn(setOf("base"))

        assertEquals(listOf("visible"), playable.map { it.text })
    }

    @Test
    fun `cards of another pack are left out`() = runBlocking {
        packs.save(CardPack("autre", "Autre"))
        punchlines.save(CatalogPunchline(CardId("p1"), "base", "base"))
        punchlines.save(CatalogPunchline(CardId("p2"), "autre", "autre"))

        assertEquals(listOf("base"), punchlines.enabledIn(setOf("base")).map { it.text })
    }

    @Test
    fun `deleting reports whether something was removed`() = runBlocking {
        punchlines.save(CatalogPunchline(CardId("p1"), "base", "à supprimer"))

        assertTrue(punchlines.delete(CardId("p1")))
        assertFalse(punchlines.delete(CardId("p1")))
        assertNull(punchlines.find(CardId("p1")))
    }

    @Test
    fun `only enabled packs are offered to a host`() = runBlocking {
        packs.save(CardPack("caché", "Caché", enabled = false))

        assertEquals(listOf("base"), packs.enabled().map { it.id })
    }
}
