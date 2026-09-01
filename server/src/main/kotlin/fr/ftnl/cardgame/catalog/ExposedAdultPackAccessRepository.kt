package fr.ftnl.cardgame.catalog

import fr.ftnl.cardgame.db.dbQuery
import fr.ftnl.cardgame.db.table.AdultPackAccessTable
import org.jetbrains.exposed.v1.core.ResultRow
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
        val updated = AdultPackAccessTable.update({ AdultPackAccessTable.discordId eq entry.discordId }) { row ->
            row[label] = entry.label
        }
        if (updated == 0) AdultPackAccessTable.insert { row ->
            row[discordId] = entry.discordId
            row[label] = entry.label
            row[addedAtMillis] = entry.addedAtMillis
        }
    }

    override suspend fun remove(discordId: String): Boolean = dbQuery {
        AdultPackAccessTable.deleteWhere { AdultPackAccessTable.discordId eq discordId } > 0
    }

    override suspend fun contains(discordId: String): Boolean = dbQuery {
        AdultPackAccessTable.selectAll()
            .where { AdultPackAccessTable.discordId eq discordId }
            .count() > 0
    }

    private fun toEntry(row: ResultRow) = AdultPackAccess(
        discordId = row[AdultPackAccessTable.discordId],
        label = row[AdultPackAccessTable.label],
        addedAtMillis = row[AdultPackAccessTable.addedAtMillis],
    )
}
