package fr.ftnl.cardgame.auth

import fr.ftnl.cardgame.config.AdminConfig

/** Decides whether a signed in Discord account may reach the administration area. */
class AdminGuard(private val config: AdminConfig) {

    fun isAdmin(user: DiscordUser): Boolean = config.grants(user.id)

    fun sessionFor(user: DiscordUser): AdminSession? =
        AdminSession(user.id, user.displayName).takeIf { isAdmin(user) }
}
