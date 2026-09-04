package fr.ftnl.cardgame.catalog

import fr.ftnl.cardgame.auth.AccountProvider
import fr.ftnl.cardgame.db.dbQuery
import fr.ftnl.cardgame.db.table.AdultPackAccessTable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

/** PostgreSQL backed implementation of [AdultPackAccessRepository]. */
class ExposedAdultPackAccessRepository : AdultPackAccessRepository {

    override suspend fun all(): List<AdultPackAccess> = dbQuery {
        AdultPackAccessTable.selectAll().orderBy(AdultPackAccessTable.addedAtMillis).map(::toEntry)
    }

    override suspend fun add(entry: AdultPackAccess): Unit = dbQuery {
        val updated = AdultPackAccessTable.update({ matches(entry.provider, entry.accountId) }) { row ->
            row[label] = entry.label
        }
        if (updated == 0) AdultPackAccessTable.insert { row ->
            row[provider] = entry.provider.name
            row[accountId] = entry.accountId
            row[label] = entry.label
            row[addedAtMillis] = entry.addedAtMillis
        }
    }

    override suspend fun remove(provider: AccountProvider, accountId: String): Boolean = dbQuery {
        AdultPackAccessTable.deleteWhere { matches(provider, accountId) } > 0
    }

    override suspend fun contains(provider: AccountProvider, accountId: String): Boolean = dbQuery {
        AdultPackAccessTable.selectAll().where { matches(provider, accountId) }.count() > 0
    }

    private fun matches(provider: AccountProvider, accountId: String) =
        (AdultPackAccessTable.provider eq provider.name) and (AdultPackAccessTable.accountId eq accountId)

    private fun toEntry(row: ResultRow) = AdultPackAccess(
        provider = AccountProvider.ofOrNull(row[AdultPackAccessTable.provider]) ?: AccountProvider.DISCORD,
        accountId = row[AdultPackAccessTable.accountId],
        label = row[AdultPackAccessTable.label],
        addedAtMillis = row[AdultPackAccessTable.addedAtMillis],
    )
}
