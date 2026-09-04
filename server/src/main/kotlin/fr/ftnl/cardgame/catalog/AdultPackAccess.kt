package fr.ftnl.cardgame.catalog

import fr.ftnl.cardgame.auth.AccountProvider

/** One account cleared to see and pick the packs marked "interdit aux mineurs". */
data class AdultPackAccess(
    val provider: AccountProvider,
    val accountId: String,
    val label: String = "",
    val addedAtMillis: Long = 0,
)
