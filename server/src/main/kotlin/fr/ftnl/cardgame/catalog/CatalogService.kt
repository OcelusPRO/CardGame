package fr.ftnl.cardgame.catalog

import fr.ftnl.cardgame.api.dto.CardPackView
import fr.ftnl.cardgame.domain.game.AnswerMode

/** Read model of the card catalogue, used by the lobby and the administration. */
class CatalogService(
    private val packs: CardPackRepository,
    private val situations: SituationCardRepository,
    private val punchlines: PunchlineCardRepository,
) {

    /**
     * The packs a host may pick from, with how many cards each one brings. When an
     * [answerMode] is given, packs that were restricted away from it are left out.
     */
    suspend fun availablePacks(answerMode: AnswerMode? = null): List<CardPackView> {
        val situationCounts = situations.all().groupingBy { it.packId }.eachCount()
        val punchlineCounts = punchlines.all().groupingBy { it.packId }.eachCount()
        return packs.enabled()
            .filter { pack -> answerMode == null || pack.allows(answerMode) }
            .map { pack ->
            CardPackView(
                id = pack.id,
                name = pack.name,
                description = pack.description,
                situationCount = situationCounts[pack.id] ?: 0,
                punchlineCount = punchlineCounts[pack.id] ?: 0,
            )
        }
    }
}
