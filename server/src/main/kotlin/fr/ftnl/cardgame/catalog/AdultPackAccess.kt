package fr.ftnl.cardgame.catalog

/** One Discord account cleared to see and pick the packs marked "interdit aux mineurs". */
data class AdultPackAccess(
    val discordId: String,
    val label: String = "",
    val addedAtMillis: Long = 0,
)
