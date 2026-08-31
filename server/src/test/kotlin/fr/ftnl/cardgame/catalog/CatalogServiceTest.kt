package fr.ftnl.cardgame.catalog

import fr.ftnl.cardgame.domain.card.CardId
import fr.ftnl.cardgame.domain.card.SituationText
import fr.ftnl.cardgame.domain.game.AnswerMode
import fr.ftnl.cardgame.support.FakeCardPackRepository
import fr.ftnl.cardgame.support.FakePunchlineCardRepository
import fr.ftnl.cardgame.support.FakeSituationCardRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class CatalogServiceTest {

    private val packs = FakeCardPackRepository(
        listOf(
            CardPack("both", "Tous modes"),
            CardPack("cardsOnly", "Cartes", answerModes = setOf(AnswerMode.CARDS)),
            CardPack("freeOnly", "Sans limites", answerModes = setOf(AnswerMode.FREE_TEXT)),
            CardPack("off", "Désactivé", enabled = false),
        )
    )
    private val situations = FakeSituationCardRepository(
        listOf(CatalogSituation(CardId("s1"), "both", SituationText("____ ?")))
    )
    private val punchlines = FakePunchlineCardRepository(
        listOf(CatalogPunchline(CardId("p1"), "cardsOnly", "un"))
    )
    private val service = CatalogService(packs, situations, punchlines)

    @Test
    fun `without a mode every enabled pack is offered`() = runBlocking {
        assertEquals(setOf("both", "cardsOnly", "freeOnly"), service.availablePacks().map { it.id }.toSet())
    }

    @Test
    fun `a mode hides the packs that were restricted away from it`() = runBlocking {
        assertEquals(
            setOf("both", "cardsOnly"),
            service.availablePacks(AnswerMode.CARDS).map { it.id }.toSet(),
        )
        assertEquals(
            setOf("both", "freeOnly"),
            service.availablePacks(AnswerMode.FREE_TEXT).map { it.id }.toSet(),
        )
    }
}
