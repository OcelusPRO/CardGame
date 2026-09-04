package fr.ftnl.cardgame.auth

import fr.ftnl.cardgame.config.AdminConfig

/** Decides whether a signed in account — Discord or Twitch — may reach the administration. */
class AdminGuard(private val config: AdminConfig) {

    fun isAdmin(account: Account): Boolean = config.grants(account)

    fun sessionFor(account: Account): AdminSession? =
        AdminSession(account.provider.name, account.id, account.displayName).takeIf { isAdmin(account) }
}
