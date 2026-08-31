package fr.ftnl.cardgame.db.table

import org.jetbrains.exposed.v1.core.Table

/** How often each official card reached a hand, was played, voted for, and won a round. */
object CardUsageTable : Table("card_usage") {
    val cardId = varchar("card_id", 64)
    val kind = varchar("kind", 16)
    val deals = long("deals").default(0)
    val plays = long("plays").default(0)
    val votes = long("votes").default(0)
    val wins = long("wins").default(0)

    override val primaryKey = PrimaryKey(cardId, kind)
}
