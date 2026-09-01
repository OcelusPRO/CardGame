package fr.ftnl.cardgame.catalog

import fr.ftnl.cardgame.domain.card.CardPool
import fr.ftnl.cardgame.domain.game.AnswerMode

/** Turns a deck request into the concrete pile of cards a game will draw from. */
class CardPoolResolver(
    private val packs: CardPackRepository,
    private val situations: SituationCardRepository,
    private val punchlines: PunchlineCardRepository,
) {

    /**
     * Exactly what was asked for: no pack selected means no official card. When an
     * [answerMode] is given, packs that were restricted away from it are dropped, so the
     * live deck follows the rules even when the host never re-applies it. When
     * [allowAdult] is false, packs marked "interdit aux mineurs" are dropped too, so a
     * host who is not cleared for them cannot slip one in over the socket.
     */
    suspend fun resolve(
        request: DeckRequest,
        answerMode: AnswerMode? = null,
        allowAdult: Boolean = true,
    ): CardPool =
        official(applicable(withoutAdult(request.packIds, allowAdult), answerMode)) +
            CardPool(request.customSituations, request.customPunchlines)

    /** What a brand new table starts with: every pack currently switched on for its mode. */
    suspend fun resolveDefault(answerMode: AnswerMode? = null): CardPool =
        official(applicable(enabledPackIds(), answerMode))

    suspend fun enabledPackIds(includeAdult: Boolean = true): Set<String> =
        packs.enabled().filter { includeAdult || !it.adultOnly }.map { it.id }.toSet()

    /** Drops the pack ids whose pack does not allow [answerMode]; keeps everything when null. */
    private suspend fun applicable(packIds: Set<String>, answerMode: AnswerMode?): Set<String> {
        if (answerMode == null || packIds.isEmpty()) return packIds
        val byId = packs.all().associateBy { it.id }
        return packIds.filterTo(mutableSetOf()) { id -> byId[id]?.allows(answerMode) != false }
    }

    /** Drops the pack ids that point at an adult-only pack, unless [allowAdult]. */
    private suspend fun withoutAdult(packIds: Set<String>, allowAdult: Boolean): Set<String> {
        if (allowAdult || packIds.isEmpty()) return packIds
        val byId = packs.all().associateBy { it.id }
        return packIds.filterTo(mutableSetOf()) { id -> byId[id]?.adultOnly != true }
    }

    private suspend fun official(packIds: Set<String>): CardPool {
        if (packIds.isEmpty()) return CardPool.EMPTY
        return CardPool(
            situations = situations.enabledIn(packIds).map { it.toDomain() },
            punchlines = punchlines.enabledIn(packIds).map { it.toDomain() },
        )
    }
}
