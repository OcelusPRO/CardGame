package fr.ftnl.cardgame.catalog

import fr.ftnl.cardgame.domain.card.CardId
import fr.ftnl.cardgame.domain.card.CardOrigin
import fr.ftnl.cardgame.domain.card.SituationText
import fr.ftnl.cardgame.support.FakeCardPackRepository
import fr.ftnl.cardgame.support.FakePunchlineCardRepository
import fr.ftnl.cardgame.support.FakeSituationCardRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CardPoolResolverTest {

    private val packs = FakeCardPackRepository(
        listOf(CardPack("base", "Base"), CardPack("nsfw", "NSFW", enabled = false)),
    )
    private val situations = FakeSituationCardRepository(
        listOf(
            CatalogSituation(CardId("s1"), "base", SituationText("____ ?")),
            CatalogSituation(CardId("s2"), "nsfw", SituationText("____ !")),
        )
    )
    private val punchlines = FakePunchlineCardRepository(
        listOf(
            CatalogPunchline(CardId("p1"), "base", "un"),
            CatalogPunchline(CardId("p2"), "nsfw", "deux"),
        )
    )
    private val resolver = CardPoolResolver(packs, situations, punchlines)

    @Test
    fun `a brand new table starts on every enabled pack`() = runBlocking {
        val pool = resolver.resolveDefault()

        assertEquals(listOf("s1"), pool.situations.map { it.id.value })
        assertEquals(listOf("p1"), pool.punchlines.map { it.id.value })
    }

    @Test
    fun `an empty selection is an explicit choice, not a default`() = runBlocking {
        val pool = resolver.resolve(DeckRequest())

        assertTrue(pool.situations.isEmpty())
        assertTrue(pool.punchlines.isEmpty())
    }

    @Test
    fun `an explicit selection wins over the enabled flag`() = runBlocking {
        val pool = resolver.resolve(DeckRequest(packIds = setOf("nsfw")))

        assertEquals(listOf("s2"), pool.situations.map { it.id.value })
    }

    @Test
    fun `custom cards are added on top of the official ones`() = runBlocking {
        val custom = CustomCardFactory()
        val pool = resolver.resolve(
            DeckRequest(
                packIds = setOf("base"),
                customSituations = custom.situations(listOf("Chez moi, ____.")),
                customPunchlines = custom.punchlines(listOf("un truc à moi")),
            )
        )

        assertEquals(2, pool.situations.size)
        assertTrue(pool.situations.any { it.origin == CardOrigin.CUSTOM })
    }

    @Test
    fun `a game without official cards keeps only what the host wrote`() = runBlocking {
        val custom = CustomCardFactory()
        val pool = resolver.resolve(
            DeckRequest(customSituations = custom.situations(listOf("Chez moi, ____.")))
        )

        assertEquals(1, pool.situations.size)
        assertTrue(pool.punchlines.isEmpty())
    }
}
