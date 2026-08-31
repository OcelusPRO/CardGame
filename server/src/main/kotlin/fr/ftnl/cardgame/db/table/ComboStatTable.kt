package fr.ftnl.cardgame.db.table

import org.jetbrains.exposed.v1.core.Table

/**
 * The heart of the statistics: which punchline was played against which situation,
 * and how well that pairing scored.
 */
object ComboStatTable : Table("combo_stats") {
    val situationId = varchar("situation_id", 64)
    val punchlineId = varchar("punchline_id", 64)
    val plays = long("plays").default(0)
    val votes = long("votes").default(0)
    val wins = long("wins").default(0)

    override val primaryKey = PrimaryKey(situationId, punchlineId)
}
