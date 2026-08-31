package fr.ftnl.cardgame.catalog

import fr.ftnl.cardgame.api.dto.PackAdminView
import fr.ftnl.cardgame.api.dto.PackInput
import fr.ftnl.cardgame.domain.game.AnswerMode
import fr.ftnl.cardgame.domain.game.GameClock
import java.util.UUID

/** Creating, editing and dropping the themed packs, from the administration area. */
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
            answerModes = answerModesOf(input),
            createdAtMillis = clock.nowMillis(),
        )
        packs.save(pack)
        return toView(pack, emptyMap(), emptyMap())
    }

    /** Refuses to drop a pack that still holds cards, so no card is ever lost by accident. */
    suspend fun delete(id: String): Boolean {
        if (holdsCards(id)) throw PackNotEmptyException(id)
        return packs.delete(id)
    }

    private suspend fun holdsCards(id: String): Boolean =
        situations.all().any { it.packId == id } || punchlines.all().any { it.packId == id }

    /** An empty choice would hide the pack everywhere, so it falls back to every mode. */
    private fun answerModesOf(input: PackInput): Set<AnswerMode> = buildSet {
        if (input.answerModeCards) add(AnswerMode.CARDS)
        if (input.answerModeFreeText) add(AnswerMode.FREE_TEXT)
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
