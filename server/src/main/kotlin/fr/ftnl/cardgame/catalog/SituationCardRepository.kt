package fr.ftnl.cardgame.catalog

import fr.ftnl.cardgame.domain.card.CardId

/** Read and write access to the official situation cards. */
interface SituationCardRepository {
    suspend fun all(): List<CatalogSituation>
    suspend fun enabledIn(packIds: Set<String>): List<CatalogSituation>
    suspend fun find(id: CardId): CatalogSituation?
    suspend fun save(card: CatalogSituation)
    suspend fun delete(id: CardId): Boolean
    suspend fun count(): Long
}
