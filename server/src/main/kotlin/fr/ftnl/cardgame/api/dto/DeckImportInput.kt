package fr.ftnl.cardgame.api.dto

import kotlinx.serialization.Serializable

/**
 * A deck pasted or uploaded from the administration: the pack settings plus its cards as
 * plain lines. With [packId] set the pack is replaced in place (its cards wiped first),
 * otherwise a brand new pack is created.
 */
@Serializable
data class DeckImportInput(
    val packId: String? = null,
    val name: String = "",
    val description: String = "",
    val answerModeCards: Boolean = true,
    val answerModeFreeText: Boolean = true,
    val situations: List<String> = emptyList(),
    val punchlines: List<String> = emptyList(),
)
