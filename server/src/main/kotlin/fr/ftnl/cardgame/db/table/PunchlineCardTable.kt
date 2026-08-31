package fr.ftnl.cardgame.db.table

import org.jetbrains.exposed.v1.core.Table

/** Official punchline cards, the funny half of the game. */
object PunchlineCardTable : Table("punchline_cards") {
    val id = varchar("id", 64)
    val packId = varchar("pack_id", 64).references(CardPackTable.id)
    val text = text("text")
    val enabled = bool("enabled").default(true)
    val createdAtMillis = long("created_at_millis")

    override val primaryKey = PrimaryKey(id)
}
