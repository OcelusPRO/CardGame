package fr.ftnl.cardgame.api.dto

import kotlinx.serialization.Serializable

/**
 * A pack as listed in the administration, with the size of what it holds.
 *
 * The catalogue model itself never crosses the wire: it is not part of the HTTP contract,
 * and sending it would break serialisation the moment it gains a field.
 */
@Serializable
data class PackAdminView(
    val id: String,
    val name: String,
    val description: String,
    val enabled: Boolean,
    val answerModeCards: Boolean,
    val answerModeFreeText: Boolean,
    val adultOnly: Boolean,
    val situationCount: Int,
    val punchlineCount: Int,
)
