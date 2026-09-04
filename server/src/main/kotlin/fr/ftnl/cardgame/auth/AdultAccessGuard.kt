package fr.ftnl.cardgame.auth

import fr.ftnl.cardgame.catalog.AdultPackAccessRepository
import fr.ftnl.cardgame.config.AdminConfig
import fr.ftnl.cardgame.config.AdultAccessConfig
import fr.ftnl.cardgame.domain.game.GameClock

/**
 * Decides whether a host may see and pick the packs marked "interdit aux mineurs".
 *
 * Access is granted when the account is an administrator, sits on the allowlist managed
 * from the administration area, or is simply old enough: an account past
 * [AdultAccessConfig.minAccountAgeDays] is trusted as an adult. Either sign in can carry
 * the clearance, and a browser holding both is judged on its best account.
 *
 * The age comes from the id itself for Discord, and from the creation date Twitch hands
 * back at sign in — so in both cases it costs no extra call and shows the host nothing.
 */
class AdultAccessGuard(
    private val access: AdultPackAccessRepository,
    private val admin: AdminConfig,
    private val clock: GameClock,
    private val config: AdultAccessConfig,
) {
    suspend fun allows(session: PlayerSession?): Boolean =
        session?.accounts().orEmpty().any { allows(it) }

    suspend fun allows(account: Account): Boolean =
        admin.grants(account) ||
            accountOldEnough(account) ||
            access.contains(account.provider, account.id)

    private fun accountOldEnough(account: Account): Boolean {
        if (!config.trustsAccountAge) return false
        val createdAtMillis = account.createdAtMillis ?: return false
        return clock.nowMillis() - createdAtMillis >= config.minAccountAgeMillis
    }
}
