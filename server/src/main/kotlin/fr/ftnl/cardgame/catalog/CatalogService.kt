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
     * Packs marked "interdit aux mineurs" are left out unless [includeAdult] is set,
     * which the caller only does for a host cleared for adult content.
     */
    suspend fun availablePacks(
        answerMode: AnswerMode? = null,
        includeAdult: Boolean = false,
    ): List<CardPackView> = view(answerMode) { pack ->
        !pack.isSecret && (includeAdult || !pack.adultOnly)
    }

    /**
     * The packs a game is actually running on, named by [ids]. Used to show a non-host
     * exactly the paquet the host built — never a pack the host could not pick — instead
     * of the viewer's own catalogue. Hidden packs stay hidden even once unlocked.
     */
    suspend fun packsInGame(ids: Set<String>, answerMode: AnswerMode? = null): List<CardPackView> {
        if (ids.isEmpty()) return emptyList()
        return view(answerMode) { pack -> pack.id in ids && !pack.isSecret }
    }

    private suspend fun view(
        answerMode: AnswerMode?,
        keep: (CardPack) -> Boolean,
    ): List<CardPackView> {
        val situationCounts = situations.all().groupingBy { it.packId }.eachCount()
        val punchlineCounts = punchlines.all().groupingBy { it.packId }.eachCount()
        return packs.enabled()
            .filter { pack -> answerMode == null || pack.allows(answerMode) }
            .filter(keep)
            .map { pack ->
                CardPackView(
                    id = pack.id,
                    name = pack.name,
                    description = pack.description,
                    situationCount = situationCounts[pack.id] ?: 0,
                    punchlineCount = punchlineCounts[pack.id] ?: 0,
                    adultOnly = pack.adultOnly,
                )
            }
    }
}
