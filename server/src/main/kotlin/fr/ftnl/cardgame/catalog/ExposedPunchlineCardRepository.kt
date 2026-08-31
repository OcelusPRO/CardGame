package fr.ftnl.cardgame.catalog

import fr.ftnl.cardgame.db.dbQuery
import fr.ftnl.cardgame.db.table.PunchlineCardTable
import fr.ftnl.cardgame.domain.card.CardId
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

/** PostgreSQL backed implementation of [PunchlineCardRepository]. */
class ExposedPunchlineCardRepository : PunchlineCardRepository {

    override suspend fun all(): List<CatalogPunchline> = dbQuery {
        PunchlineCardTable.selectAll().map(::toCard)
    }

    override suspend fun enabledIn(packIds: Set<String>): List<CatalogPunchline> = dbQuery {
        PunchlineCardTable.selectAll()
            .where { (PunchlineCardTable.enabled eq true) and (PunchlineCardTable.packId inList packIds) }
            .map(::toCard)
    }

    override suspend fun find(id: CardId): CatalogPunchline? = dbQuery {
        PunchlineCardTable.selectAll().where { PunchlineCardTable.id eq id.value }.map(::toCard).firstOrNull()
    }

    override suspend fun save(card: CatalogPunchline): Unit = dbQuery {
        val updated = PunchlineCardTable.update({ PunchlineCardTable.id eq card.id.value }) { row ->
            row[packId] = card.packId
            row[text] = card.text
            row[enabled] = card.enabled
        }
        if (updated == 0) insert(card)
    }

    override suspend fun delete(id: CardId): Boolean = dbQuery {
        PunchlineCardTable.deleteWhere { PunchlineCardTable.id eq id.value } > 0
    }

    override suspend fun count(): Long = dbQuery { PunchlineCardTable.selectAll().count() }

    private fun insert(card: CatalogPunchline) {
        PunchlineCardTable.insert { row ->
            row[id] = card.id.value
            row[packId] = card.packId
            row[text] = card.text
            row[enabled] = card.enabled
            row[createdAtMillis] = card.createdAtMillis
        }
    }

    private fun toCard(row: ResultRow) = CatalogPunchline(
        id = CardId(row[PunchlineCardTable.id]),
        packId = row[PunchlineCardTable.packId],
        text = row[PunchlineCardTable.text],
        enabled = row[PunchlineCardTable.enabled],
        createdAtMillis = row[PunchlineCardTable.createdAtMillis],
    )
}
