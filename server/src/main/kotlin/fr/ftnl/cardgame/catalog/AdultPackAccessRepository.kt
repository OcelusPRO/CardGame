package fr.ftnl.cardgame.catalog

import fr.ftnl.cardgame.auth.AccountProvider

/** The allowlist of accounts cleared for adult-only packs. */
interface AdultPackAccessRepository {
    suspend fun all(): List<AdultPackAccess>
    suspend fun add(entry: AdultPackAccess)
    suspend fun remove(provider: AccountProvider, accountId: String): Boolean
    suspend fun contains(provider: AccountProvider, accountId: String): Boolean
}
