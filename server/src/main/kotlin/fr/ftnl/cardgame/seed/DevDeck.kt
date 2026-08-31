package fr.ftnl.cardgame.seed

import kotlinx.serialization.Serializable

/** Shape of the bundled `dev-deck.json`, used to fill a fresh database for testing. */
@Serializable
data class DevDeck(
    val packId: String,
    val packName: String,
    val packDescription: String = "",
    val situations: List<String> = emptyList(),
    val punchlines: List<String> = emptyList(),
)
