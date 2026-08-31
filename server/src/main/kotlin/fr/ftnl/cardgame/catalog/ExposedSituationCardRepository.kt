package fr.ftnl.cardgame.catalog

import fr.ftnl.cardgame.db.dbQuery
import fr.ftnl.cardgame.db.table.SituationCardTable
import fr.ftnl.cardgame.domain.card.CardId
import fr.ftnl.cardgame.domain.card.SituationText
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

/** PostgreSQL backed implementation of [SituationCardRepository]. */
class ExposedSituationCardRepository : SituationCardRepository {

    override suspend fun all(): List<CatalogSituation> = dbQuery {
        SituationCardTable.selectAll().map(::toCard)
    }

    override suspend fun enabledIn(packIds: Set<String>): List<CatalogSituation> = dbQuery {
        SituationCardTable.selectAll()
            .where { (SituationCardTable.enabled eq true) and (SituationCardTable.packId inList packIds) }
            .map(::toCard)
    }

    override suspend fun find(id: CardId): CatalogSituation? = dbQuery {
        SituationCardTable.selectAll().where { SituationCardTable.id eq id.value }.map(::toCard).firstOrNull()
    }

    override suspend fun save(card: CatalogSituation): Unit = dbQuery {
        val updated = SituationCardTable.update({ SituationCardTable.id eq card.id.value }) { row ->
            row[packId] = card.packId
            row[text] = card.text.raw
            row[enabled] = card.enabled
        }
        if (updated == 0) insert(card)
    }

    override suspend fun delete(id: CardId): Boolean = dbQuery {
        SituationCardTable.deleteWhere { SituationCardTable.id eq id.value } > 0
    }

    override suspend fun count(): Long = dbQuery { SituationCardTable.selectAll().count() }

    private fun insert(card: CatalogSituation) {
        SituationCardTable.insert { row ->
            row[id] = card.id.value
            row[packId] = card.packId
            row[text] = card.text.raw
            row[enabled] = card.enabled
            row[createdAtMillis] = card.createdAtMillis
        }
    }

    private fun toCard(row: ResultRow) = CatalogSituation(
        id = CardId(row[SituationCardTable.id]),
        packId = row[SituationCardTable.packId],
        text = SituationText(row[SituationCardTable.text]),
        enabled = row[SituationCardTable.enabled],
        createdAtMillis = row[SituationCardTable.createdAtMillis],
    )
}
