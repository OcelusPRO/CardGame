package fr.ftnl.cardgame.db.table

import org.jetbrains.exposed.v1.core.Table

/** Accounts cleared to see and pick the packs marked "interdit aux mineurs". */
object AdultPackAccessTable : Table("adult_pack_access") {
    val provider = varchar("provider", 16)
    val accountId = varchar("account_id", 64)
    val label = varchar("label", 120).default("")
    val addedAtMillis = long("added_at_millis")

    /** An id only means something next to the provider that handed it out. */
    override val primaryKey = PrimaryKey(provider, accountId)
}
