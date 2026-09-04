package fr.ftnl.cardgame.catalog

import fr.ftnl.cardgame.api.dto.AdultAccessInput
import fr.ftnl.cardgame.api.dto.AdultAccessView
import fr.ftnl.cardgame.auth.AccountProvider
import fr.ftnl.cardgame.domain.game.GameClock

/** Managing the allowlist of accounts cleared for adult-only packs. */
class AdultAccessService(
    private val access: AdultPackAccessRepository,
    private val clock: GameClock,
) {

    suspend fun all(): List<AdultAccessView> = access.all().map(::toView)

    suspend fun add(input: AdultAccessInput): AdultAccessView {
        val provider = AccountProvider.ofOrNull(input.provider)
        requireNotNull(provider) { "Fournisseur inconnu : ${input.provider}" }
        val accountId = input.accountId.trim()
        require(accountId.isNotEmpty()) { "L'identifiant est vide" }
        // Both providers hand out plain numbers; a channel name or a pseudo would silently
        // never match anything, so it is refused here rather than at the first game.
        require(accountId.all { it.isDigit() }) {
            "Un identifiant ${provider.name.lowercase()} ne contient que des chiffres"
        }
        val existing = access.all().firstOrNull { it.provider == provider && it.accountId == accountId }
        val entry = AdultPackAccess(
            provider = provider,
            accountId = accountId,
            label = input.label.trim(),
            addedAtMillis = existing?.addedAtMillis ?: clock.nowMillis(),
        )
        access.add(entry)
        return toView(entry)
    }

    suspend fun remove(provider: String, accountId: String): Boolean {
        val known = AccountProvider.ofOrNull(provider) ?: return false
        return access.remove(known, accountId.trim())
    }

    private fun toView(entry: AdultPackAccess) =
        AdultAccessView(entry.provider.name, entry.accountId, entry.label, entry.addedAtMillis)
}
