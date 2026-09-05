package fr.ftnl.cardgame.config

import fr.ftnl.cardgame.auth.Account
import fr.ftnl.cardgame.auth.AccountProvider

/**
 * The administration area is open to these accounts, and to nobody else. The two lists
 * are kept apart on purpose: both providers hand out plain numbers, so an id is only ever
 * compared with the ones of its own provider.
 *
 * [twitchIds] also accepts a **channel name**, which is the handle a streamer actually
 * knows: `ADMIN_TWITCH_IDS=kameto` works as well as the number behind it.
 */
data class AdminConfig(
    val discordIds: Set<String>,
    val twitchIds: Set<String> = emptySet(),
) {
    fun grants(account: Account): Boolean = when (account.provider) {
        AccountProvider.DISCORD -> account.id in discordIds
        AccountProvider.TWITCH -> twitchIds.any { it.matches(account) }
    }

    private fun String.matches(account: Account): Boolean =
        equals(account.id, ignoreCase = true) ||
            (account.login != null && equals(account.login, ignoreCase = true))
}
