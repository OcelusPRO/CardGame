package fr.ftnl.cardgame.catalog

import fr.ftnl.cardgame.api.dto.DeckImportInput
import fr.ftnl.cardgame.api.dto.PackAdminView
import fr.ftnl.cardgame.api.dto.PackInput
import fr.ftnl.cardgame.domain.card.CardId
import fr.ftnl.cardgame.domain.card.SituationText
import fr.ftnl.cardgame.domain.game.AnswerMode
import fr.ftnl.cardgame.domain.game.GameClock
import java.util.UUID

/** Creating, editing, importing and dropping the themed packs, from the administration area. */
class AdminPackService(
    private val packs: CardPackRepository,
    private val situations: SituationCardRepository,
    private val punchlines: PunchlineCardRepository,
    private val clock: GameClock,
) {

    suspend fun all(): List<PackAdminView> {
        val situationCounts = situations.all().groupingBy { it.packId }.eachCount()
        val punchlineCounts = punchlines.all().groupingBy { it.packId }.eachCount()
        return packs.all().map { pack -> toView(pack, situationCounts, punchlineCounts) }
    }

    suspend fun save(input: PackInput): PackAdminView {
        val pack = CardPack(
            id = input.id ?: UUID.randomUUID().toString(),
            name = input.name.trim().ifEmpty { throw IllegalArgumentException("Le nom du pack est vide") },
            description = input.description.trim(),
            enabled = input.enabled,
            answerModes = answerModesOf(input.answerModeCards, input.answerModeFreeText),
            createdAtMillis = clock.nowMillis(),
        )
        packs.save(pack)
        return toView(pack, emptyMap(), emptyMap())
    }

    /**
     * Turns a pasted deck into a pack and its cards in one go. Blank lines are dropped.
     * Replacing an existing pack ([DeckImportInput.packId]) wipes its cards first so the
     * import is the new truth, not an append.
     */
    suspend fun import(input: DeckImportInput): PackAdminView {
        val situationLines = input.situations.map { it.trim() }.filter { it.isNotEmpty() }
        val punchlineLines = input.punchlines.map { it.trim() }.filter { it.isNotEmpty() }
        require(situationLines.isNotEmpty() || punchlineLines.isNotEmpty()) {
            "Le deck importé ne contient aucune carte"
        }
        val name = input.name.trim().ifEmpty { throw IllegalArgumentException("Le nom du pack est vide") }

        val existing = input.packId?.let { id -> packs.all().firstOrNull { it.id == id } }
        val pack = CardPack(
            id = existing?.id ?: UUID.randomUUID().toString(),
            name = name,
            description = input.description.trim(),
            enabled = existing?.enabled ?: true,
            answerModes = answerModesOf(input.answerModeCards, input.answerModeFreeText),
            createdAtMillis = existing?.createdAtMillis ?: clock.nowMillis(),
        )
        packs.save(pack)
        if (existing != null) wipeCards(pack.id)

        situationLines.forEach { line ->
            situations.save(
                CatalogSituation(CardId(newId("s")), pack.id, SituationText(line), true, clock.nowMillis()),
            )
        }
        punchlineLines.forEach { line ->
            punchlines.save(
                CatalogPunchline(CardId(newId("p")), pack.id, line, true, clock.nowMillis()),
            )
        }
        return toView(
            pack,
            mapOf(pack.id to situationLines.size),
            mapOf(pack.id to punchlineLines.size),
        )
    }

    /** Refuses to drop a pack that still holds cards, so no card is ever lost by accident. */
    suspend fun delete(id: String): Boolean {
        if (holdsCards(id)) throw PackNotEmptyException(id)
        return packs.delete(id)
    }

    private suspend fun holdsCards(id: String): Boolean =
        situations.all().any { it.packId == id } || punchlines.all().any { it.packId == id }

    private suspend fun wipeCards(packId: String) {
        situations.all().filter { it.packId == packId }.forEach { situations.delete(it.id) }
        punchlines.all().filter { it.packId == packId }.forEach { punchlines.delete(it.id) }
    }

    private fun newId(prefix: String) = "$prefix-${UUID.randomUUID()}"

    /** An empty choice would hide the pack everywhere, so it falls back to every mode. */
    private fun answerModesOf(cards: Boolean, freeText: Boolean): Set<AnswerMode> = buildSet {
        if (cards) add(AnswerMode.CARDS)
        if (freeText) add(AnswerMode.FREE_TEXT)
    }.ifEmpty { AnswerMode.entries.toSet() }

    private fun toView(
        pack: CardPack,
        situationCounts: Map<String, Int>,
        punchlineCounts: Map<String, Int>,
    ) = PackAdminView(
        id = pack.id,
        name = pack.name,
        description = pack.description,
        enabled = pack.enabled,
        answerModeCards = pack.allows(AnswerMode.CARDS),
        answerModeFreeText = pack.allows(AnswerMode.FREE_TEXT),
        situationCount = situationCounts[pack.id] ?: 0,
        punchlineCount = punchlineCounts[pack.id] ?: 0,
    )
}
