package fr.ftnl.cardgame.config

import fr.ftnl.cardgame.auth.Account
import fr.ftnl.cardgame.auth.AccountProvider

/**
 * The administration area is open to these accounts, and to nobody else. The two lists
 * are kept apart on purpose: both providers hand out plain numbers, so an id is only
 * ever compared with the ones of its own provider.
 */
data class AdminConfig(
    val discordIds: Set<String>,
    val twitchIds: Set<String> = emptySet(),
) {
    fun grants(account: Account): Boolean = account.id in idsOf(account.provider)

    private fun idsOf(provider: AccountProvider): Set<String> = when (provider) {
        AccountProvider.DISCORD -> discordIds
        AccountProvider.TWITCH -> twitchIds
    }
}
