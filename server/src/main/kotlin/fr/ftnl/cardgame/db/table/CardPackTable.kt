package fr.ftnl.cardgame.db.table

import org.jetbrains.exposed.v1.core.Table

/** A named group of official cards, so the host can pick themes before starting. */
object CardPackTable : Table("card_packs") {
    val id = varchar("id", 64)
    val name = varchar("name", 120)
    val description = text("description")
    val enabled = bool("enabled").default(true)
    val answerModeCards = bool("answer_mode_cards").default(true)
    val answerModeFreeText = bool("answer_mode_free_text").default(true)
    val createdAtMillis = long("created_at_millis")

    override val primaryKey = PrimaryKey(id)
}
