package fr.ftnl.cardgame.catalog

import fr.ftnl.cardgame.api.dto.AdultAccessInput
import fr.ftnl.cardgame.api.dto.AdultAccessView
import fr.ftnl.cardgame.domain.game.GameClock

/** Managing the allowlist of Discord accounts cleared for adult-only packs. */
class AdultAccessService(
    private val access: AdultPackAccessRepository,
    private val clock: GameClock,
) {

    suspend fun all(): List<AdultAccessView> = access.all().map { entry ->
        AdultAccessView(entry.discordId, entry.label, entry.addedAtMillis)
    }

    suspend fun add(input: AdultAccessInput): AdultAccessView {
        val discordId = input.discordId.trim()
        require(discordId.isNotEmpty()) { "L'identifiant Discord est vide" }
        require(discordId.all { it.isDigit() }) { "Un identifiant Discord ne contient que des chiffres" }
        val existing = access.all().firstOrNull { it.discordId == discordId }
        val entry = AdultPackAccess(
            discordId = discordId,
            label = input.label.trim(),
            addedAtMillis = existing?.addedAtMillis ?: clock.nowMillis(),
        )
        access.add(entry)
        return AdultAccessView(entry.discordId, entry.label, entry.addedAtMillis)
    }

    suspend fun remove(discordId: String): Boolean = access.remove(discordId.trim())
}
