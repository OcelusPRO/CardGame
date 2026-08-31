package fr.ftnl.cardgame.api.dto

import kotlinx.serialization.Serializable

/**
 * The deck the host picked: the official packs they ticked, plus the cards they wrote in
 * the browser. An empty [packIds] means a game played on custom cards only.
 * Custom cards are stored in the game session, never in the database.
 */
@Serializable
data class DeckInput(
    val packIds: Set<String> = emptySet(),
    val customSituations: List<String> = emptyList(),
    val customPunchlines: List<String> = emptyList(),
)
