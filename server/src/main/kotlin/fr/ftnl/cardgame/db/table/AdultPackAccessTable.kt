package fr.ftnl.cardgame.db.table

import org.jetbrains.exposed.v1.core.Table

/** Discord accounts cleared to see and pick the packs marked "interdit aux mineurs". */
object AdultPackAccessTable : Table("adult_pack_access") {
    val discordId = varchar("discord_id", 64)
    val label = varchar("label", 120).default("")
    val addedAtMillis = long("added_at_millis")

    override val primaryKey = PrimaryKey(discordId)
}
