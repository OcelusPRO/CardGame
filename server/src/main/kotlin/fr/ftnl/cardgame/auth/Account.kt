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
 * An account, whichever provider it came from. Ids are only ever compared within a
 * provider: both hand out plain numbers, and nothing says a Discord id and a Twitch id
 * cannot collide.
 *
 * [login] is the Twitch channel name — the handle everybody actually knows, where the id
 * is a number nobody has ever seen. [createdAtMillis] is when the account was opened,
 * when the provider lets us know: it is what the adult-pack age rule reads, and it is
 * never shown to anybody.
 */
data class Account(
    val provider: AccountProvider,
    val id: String,
    val displayName: String = "",
    val login: String? = null,
    val avatarUrl: String? = null,
    val createdAtMillis: Long? = null,
)

/**
 * Finds the account behind what an administrator typed: an id, or — on Twitch — a channel
 * name. Null when nothing matches, or when the server has no way to ask.
 */
fun interface AccountLookup {
    suspend fun find(provider: AccountProvider, query: String): Account?

    companion object {
        /** What the game uses when neither provider can be queried. */
        val NONE = AccountLookup { _, _ -> null }
    }
}
