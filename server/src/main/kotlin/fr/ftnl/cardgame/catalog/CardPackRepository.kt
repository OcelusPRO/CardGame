package fr.ftnl.cardgame.catalog

/** Read and write access to the themed packs of official cards. */
interface CardPackRepository {
    suspend fun all(): List<CardPack>
    suspend fun enabled(): List<CardPack>
    suspend fun save(pack: CardPack)
    suspend fun delete(id: String): Boolean
}
