package fr.ftnl.cardgame.auth

/** Where a signed in account comes from. */
enum class AccountProvider {
    DISCORD,
    TWITCH,
    ;

    companion object {
        /** Null rather than an exception: the value often comes from a URL or a payload. */
        fun ofOrNull(raw: String?): AccountProvider? =
            entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) }
    }
}

/**
 * A signed in account, whichever provider it came from. Ids are only ever compared
 * within a provider: both hand out plain numbers, and nothing says a Discord id and a
 * Twitch id cannot collide.
 *
 * [createdAtMillis] is when the account was opened, when the provider lets us know: it
 * is what the adult-pack age rule reads, and it is never shown to anybody.
 */
data class Account(
    val provider: AccountProvider,
    val id: String,
    val displayName: String = "",
    val createdAtMillis: Long? = null,
)
