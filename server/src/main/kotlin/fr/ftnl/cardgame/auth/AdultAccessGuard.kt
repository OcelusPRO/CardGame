package fr.ftnl.cardgame.auth

import fr.ftnl.cardgame.catalog.AdultPackAccessRepository
import fr.ftnl.cardgame.config.AdminConfig
import fr.ftnl.cardgame.config.AdultAccessConfig
import fr.ftnl.cardgame.domain.game.GameClock

/**
 * Decides whether a host may see and pick the packs marked "interdit aux mineurs".
 *
 * Access is granted when the Discord account is an administrator, sits on the allowlist
 * managed from the administration area, or is simply old enough: an account past
 * [AdultAccessConfig.minAccountAgeDays] is trusted as an adult. That last check reads the
 * creation date straight out of the id, so it costs nothing and shows the host nothing.
 */
class AdultAccessGuard(
    private val access: AdultPackAccessRepository,
    private val admin: AdminConfig,
    private val clock: GameClock,
    private val config: AdultAccessConfig,
) {
    suspend fun allows(discordId: String?): Boolean {
        if (discordId.isNullOrBlank()) return false
        return admin.grants(discordId) ||
            accountOldEnough(discordId) ||
            access.contains(discordId)
    }

    private fun accountOldEnough(discordId: String): Boolean {
        if (!config.trustsAccountAge) return false
        val createdAtMillis = DiscordSnowflake.createdAtMillis(discordId) ?: return false
        return clock.nowMillis() - createdAtMillis >= config.minAccountAgeMillis
    }
}
