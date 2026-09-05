package fr.ftnl.cardgame.catalog

import fr.ftnl.cardgame.api.dto.AdultAccessInput
import fr.ftnl.cardgame.api.dto.AdultAccessView
import fr.ftnl.cardgame.auth.AccountLookup
import fr.ftnl.cardgame.auth.AccountProvider
import fr.ftnl.cardgame.auth.UnknownAccountException
import fr.ftnl.cardgame.domain.game.GameClock

/**
 * Managing the allowlist of accounts cleared for adult-only packs.
 *
 * What gets stored is always the account id, but what gets typed need not be: on Twitch a
 * channel name is resolved to the id behind it, and on both providers the pseudo is read
 * from the profile so the list stays readable without anybody filling a label in.
 */
class AdultAccessService(
    private val access: AdultPackAccessRepository,
    private val clock: GameClock,
    private val accounts: AccountLookup = AccountLookup.NONE,
) {

    suspend fun all(): List<AdultAccessView> = access.all().map(::toView)

    suspend fun add(input: AdultAccessInput): AdultAccessView {
        val provider = AccountProvider.ofOrNull(input.provider)
        requireNotNull(provider) { "Fournisseur inconnu : ${input.provider}" }
        val asked = input.accountId.trim().removePrefix("@")
        require(asked.isNotEmpty()) { "L'identifiant est vide" }

        val found = accounts.find(provider, asked)
        // Without a lookup — no bot token, no Twitch credentials — a plain id still works,
        // and only that: a pseudo nobody can resolve would never match a single player.
        val accountId = found?.id ?: asked.takeIf { id -> id.all(Char::isDigit) }
            ?: throw UnknownAccountException("Aucun compte ${provider.name.lowercase()} ne répond à « $asked »")

        val existing = access.all().firstOrNull { it.provider == provider && it.accountId == accountId }
        val entry = AdultPackAccess(
            provider = provider,
            accountId = accountId,
            label = input.label.trim().ifEmpty { found?.displayName.orEmpty() },
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
