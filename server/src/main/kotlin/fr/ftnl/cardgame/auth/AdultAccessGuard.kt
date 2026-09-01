package fr.ftnl.cardgame.auth

import fr.ftnl.cardgame.catalog.AdultPackAccessRepository
import fr.ftnl.cardgame.config.AdminConfig

/**
 * Decides whether a host may see and pick the packs marked "interdit aux mineurs".
 * Access needs a Discord account that is either an administrator or on the allowlist
 * managed from the administration area.
 */
class AdultAccessGuard(
    private val access: AdultPackAccessRepository,
    private val admin: AdminConfig,
) {
    suspend fun allows(discordId: String?): Boolean {
        if (discordId.isNullOrBlank()) return false
        return admin.grants(discordId) || access.contains(discordId)
    }
}
