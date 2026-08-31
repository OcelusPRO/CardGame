package fr.ftnl.cardgame.stats

/** One point of the admin activity chart. */
data class DailyActivity(
    val day: String,
    val gamesCreated: Long,
    val roundsPlayed: Long,
    val answersPlayed: Long,
)
