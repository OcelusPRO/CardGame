package fr.ftnl.cardgame.catalog

import fr.ftnl.cardgame.db.dbQuery
import fr.ftnl.cardgame.db.table.CardPackTable
import fr.ftnl.cardgame.domain.game.AnswerMode
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

/** PostgreSQL backed implementation of [CardPackRepository]. */
class ExposedCardPackRepository : CardPackRepository {

    override suspend fun all(): List<CardPack> = dbQuery {
        CardPackTable.selectAll().orderBy(CardPackTable.name).map(::toPack)
    }

    override suspend fun enabled(): List<CardPack> = dbQuery {
        CardPackTable.selectAll().where { CardPackTable.enabled eq true }.map(::toPack)
    }

    override suspend fun save(pack: CardPack): Unit = dbQuery {
        val updated = CardPackTable.update({ CardPackTable.id eq pack.id }) { row ->
            row[name] = pack.name
            row[description] = pack.description
            row[enabled] = pack.enabled
            row[answerModeCards] = pack.allows(AnswerMode.CARDS)
            row[answerModeFreeText] = pack.allows(AnswerMode.FREE_TEXT)
            row[adultOnly] = pack.adultOnly
        }
        if (updated == 0) insert(pack)
    }

    override suspend fun delete(id: String): Boolean = dbQuery {
        CardPackTable.deleteWhere { CardPackTable.id eq id } > 0
    }

    private fun insert(pack: CardPack) {
        CardPackTable.insert { row ->
            row[id] = pack.id
            row[name] = pack.name
            row[description] = pack.description
            row[enabled] = pack.enabled
            row[answerModeCards] = pack.allows(AnswerMode.CARDS)
            row[answerModeFreeText] = pack.allows(AnswerMode.FREE_TEXT)
            row[adultOnly] = pack.adultOnly
            row[createdAtMillis] = pack.createdAtMillis
        }
    }

    private fun toPack(row: ResultRow) = CardPack(
        id = row[CardPackTable.id],
        name = row[CardPackTable.name],
        description = row[CardPackTable.description],
        enabled = row[CardPackTable.enabled],
        adultOnly = row[CardPackTable.adultOnly],
        answerModes = buildSet {
            if (row[CardPackTable.answerModeCards]) add(AnswerMode.CARDS)
            if (row[CardPackTable.answerModeFreeText]) add(AnswerMode.FREE_TEXT)
        },
        createdAtMillis = row[CardPackTable.createdAtMillis],
    )
}
