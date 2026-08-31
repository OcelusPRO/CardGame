package fr.ftnl.cardgame.db.table

import org.jetbrains.exposed.v1.core.Table

/** One row per day, which is all the admin charts need to draw a trend. */
object DailyActivityTable : Table("daily_activity") {
    val day = varchar("activity_day", 10)
    val gamesCreated = long("games_created").default(0)
    val roundsPlayed = long("rounds_played").default(0)
    val answersPlayed = long("answers_played").default(0)

    override val primaryKey = PrimaryKey(day)
}
