package fr.ftnl.cardgame.catalog

import fr.ftnl.cardgame.domain.card.CardId

/** Read and write access to the official punchline cards. */
interface PunchlineCardRepository {
    suspend fun all(): List<CatalogPunchline>
    suspend fun enabledIn(packIds: Set<String>): List<CatalogPunchline>
    suspend fun find(id: CardId): CatalogPunchline?
    suspend fun save(card: CatalogPunchline)
    suspend fun delete(id: CardId): Boolean
    suspend fun count(): Long
}
