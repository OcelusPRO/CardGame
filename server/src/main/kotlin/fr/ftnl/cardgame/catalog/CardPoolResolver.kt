package fr.ftnl.cardgame.catalog

import fr.ftnl.cardgame.domain.card.CardPool

/** Turns a deck request into the concrete pile of cards a game will draw from. */
class CardPoolResolver(
    private val packs: CardPackRepository,
    private val situations: SituationCardRepository,
    private val punchlines: PunchlineCardRepository,
) {

    /** Exactly what was asked for: no pack selected means no official card. */
    suspend fun resolve(request: DeckRequest): CardPool =
        official(request.packIds) + CardPool(request.customSituations, request.customPunchlines)

    /** What a brand new table starts with: every pack currently switched on. */
    suspend fun resolveDefault(): CardPool = official(enabledPackIds())

    suspend fun enabledPackIds(): Set<String> = packs.enabled().map { it.id }.toSet()

    private suspend fun official(packIds: Set<String>): CardPool {
        if (packIds.isEmpty()) return CardPool.EMPTY
        return CardPool(
            situations = situations.enabledIn(packIds).map { it.toDomain() },
            punchlines = punchlines.enabledIn(packIds).map { it.toDomain() },
        )
    }
}
