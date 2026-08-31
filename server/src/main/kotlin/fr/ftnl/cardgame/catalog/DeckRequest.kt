package fr.ftnl.cardgame.catalog

import fr.ftnl.cardgame.domain.card.PunchlineCard
import fr.ftnl.cardgame.domain.card.SituationCard

/**
 * What the host wants to play with: the official packs they selected, plus the cards they
 * wrote themselves. An empty [packIds] is an explicit choice, not a default: the game is
 * then played on custom cards alone. Custom cards never leave the session that carries them.
 */
data class DeckRequest(
    val packIds: Set<String> = emptySet(),
    val customSituations: List<SituationCard> = emptyList(),
    val customPunchlines: List<PunchlineCard> = emptyList(),
)
