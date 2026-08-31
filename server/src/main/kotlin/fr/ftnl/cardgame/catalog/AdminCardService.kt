package fr.ftnl.cardgame.catalog

import fr.ftnl.cardgame.api.dto.CardAdminView
import fr.ftnl.cardgame.api.dto.CardInput
import fr.ftnl.cardgame.domain.card.CardId
import fr.ftnl.cardgame.domain.card.SituationText
import fr.ftnl.cardgame.domain.game.GameClock
import java.util.UUID

/** Creating, editing and dropping the official cards, from the administration area. */
class AdminCardService(
    private val situations: SituationCardRepository,
    private val punchlines: PunchlineCardRepository,
    private val clock: GameClock,
) {

    suspend fun allSituations(): List<CardAdminView> = situations.all().map { card ->
        CardAdminView(card.id.value, card.packId, card.text.raw, card.enabled, card.text.blankCount)
    }

    suspend fun allPunchlines(): List<CardAdminView> = punchlines.all().map { card ->
        CardAdminView(card.id.value, card.packId, card.text, card.enabled)
    }

    suspend fun saveSituation(input: CardInput): CardAdminView {
        val card = CatalogSituation(
            id = CardId(input.id ?: newId("s")),
            packId = input.packId,
            text = SituationText(input.text.trim()),
            enabled = input.enabled,
            createdAtMillis = clock.nowMillis(),
        )
        situations.save(card)
        return CardAdminView(card.id.value, card.packId, card.text.raw, card.enabled, card.text.blankCount)
    }

    suspend fun savePunchline(input: CardInput): CardAdminView {
        val text = input.text.trim()
        require(text.isNotEmpty()) { "Le texte de la carte est vide" }
        val card = CatalogPunchline(
            id = CardId(input.id ?: newId("p")),
            packId = input.packId,
            text = text,
            enabled = input.enabled,
            createdAtMillis = clock.nowMillis(),
        )
        punchlines.save(card)
        return CardAdminView(card.id.value, card.packId, card.text, card.enabled)
    }

    suspend fun deleteSituation(id: String): Boolean = situations.delete(CardId(id))

    suspend fun deletePunchline(id: String): Boolean = punchlines.delete(CardId(id))

    private fun newId(prefix: String) = "$prefix-${UUID.randomUUID()}"
}
