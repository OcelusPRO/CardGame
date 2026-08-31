package fr.ftnl.cardgame.catalog

import fr.ftnl.cardgame.api.dto.DeckImportInput
import fr.ftnl.cardgame.domain.card.CardId
import fr.ftnl.cardgame.domain.card.SituationText
import fr.ftnl.cardgame.domain.game.AnswerMode
import fr.ftnl.cardgame.domain.game.GameClock
import fr.ftnl.cardgame.support.FakeCardPackRepository
import fr.ftnl.cardgame.support.FakePunchlineCardRepository
import fr.ftnl.cardgame.support.FakeSituationCardRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AdminPackServiceTest {

    private val clock = GameClock { 1_000L }
    private val packs = FakeCardPackRepository()
    private val situations = FakeSituationCardRepository()
    private val punchlines = FakePunchlineCardRepository()
    private val service = AdminPackService(packs, situations, punchlines, clock)

    @Test
    fun `import creates a pack and its cards, blank lines dropped`() = runBlocking {
        val view = service.import(
            DeckImportInput(
                name = "  Fêtes  ",
                description = "cartes de Noël",
                answerModeCards = true,
                answerModeFreeText = false,
                situations = listOf("Le pire cadeau, c'est ____.", "  ", "____ sous le sapin."),
                punchlines = listOf("un pull moche", ""),
            ),
        )

        assertEquals("Fêtes", view.name)
        assertEquals(2, view.situationCount)
        assertEquals(1, view.punchlineCount)
        assertEquals(true, view.answerModeCards)
        assertEquals(false, view.answerModeFreeText)
        assertEquals(2, situations.all().count { it.packId == view.id })
        assertEquals(1, punchlines.all().count { it.packId == view.id })
    }

    @Test
    fun `importing onto an existing pack wipes its old cards first`() = runBlocking {
        packs.save(CardPack("base", "Base"))
        situations.save(CatalogSituation(CardId("old-s"), "base", SituationText("ancienne ____.")))
        punchlines.save(CatalogPunchline(CardId("old-p"), "base", "ancienne réponse"))

        val view = service.import(
            DeckImportInput(
                packId = "base",
                name = "Base",
                situations = listOf("nouvelle ____."),
                punchlines = listOf("nouvelle réponse", "et une autre"),
            ),
        )

        assertEquals("base", view.id)
        assertEquals(1, view.situationCount)
        assertEquals(2, view.punchlineCount)
        assertTrue(situations.all().none { it.id == CardId("old-s") })
        assertTrue(punchlines.all().none { it.id == CardId("old-p") })
    }

    @Test
    fun `a replaced pack keeps its enabled flag`() = runBlocking {
        packs.save(CardPack("hidden", "Caché", enabled = false))

        val view = service.import(
            DeckImportInput(packId = "hidden", name = "Caché", punchlines = listOf("x")),
        )

        assertEquals(false, view.enabled)
    }

    @Test
    fun `an empty deck is refused`() {
        assertFailsWith<IllegalArgumentException> {
            runBlocking { service.import(DeckImportInput(name = "Vide")) }
        }
    }

    @Test
    fun `a new pack without a name is refused`() {
        assertFailsWith<IllegalArgumentException> {
            runBlocking { service.import(DeckImportInput(name = "  ", punchlines = listOf("x"))) }
        }
    }

    @Test
    fun `an empty mode choice falls back to every mode`() = runBlocking {
        val view = service.import(
            DeckImportInput(
                name = "Tous",
                answerModeCards = false,
                answerModeFreeText = false,
                punchlines = listOf("x"),
            ),
        )

        assertTrue(view.answerModeCards)
        assertTrue(view.answerModeFreeText)
    }
}
