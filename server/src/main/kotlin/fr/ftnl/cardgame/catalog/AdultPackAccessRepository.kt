package fr.ftnl.cardgame.catalog

/** The allowlist of Discord accounts cleared for adult-only packs. */
interface AdultPackAccessRepository {
    suspend fun all(): List<AdultPackAccess>
    suspend fun add(entry: AdultPackAccess)
    suspend fun remove(discordId: String): Boolean
    suspend fun contains(discordId: String): Boolean
}
