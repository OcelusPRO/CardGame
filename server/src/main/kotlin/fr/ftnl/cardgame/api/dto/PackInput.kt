package fr.ftnl.cardgame.api.dto

import kotlinx.serialization.Serializable

/** Payload used to create or edit a pack from the administration. */
@Serializable
data class PackInput(
    val id: String? = null,
    val name: String,
    val description: String = "",
    val enabled: Boolean = true,
    /** Answer modes the pack may be played in; both true (the default) means no limit. */
    val answerModeCards: Boolean = true,
    val answerModeFreeText: Boolean = true,
    /** Marks the pack "interdit aux mineurs". */
    val adultOnly: Boolean = false,
    /**
     * When non-blank the pack is hidden from the lobby and only enters a game when the
     * host types this code into the situations box. Blank or null keeps the pack public.
     */
    val secretCode: String? = null,
)
