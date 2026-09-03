package fr.ftnl.cardgame.catalog

import fr.ftnl.cardgame.domain.game.AnswerMode

/** A themed group of official cards the host can switch on or off before a game. */
data class CardPack(
    val id: String,
    val name: String,
    val description: String = "",
    val enabled: Boolean = true,
    /** When true the pack is hidden from hosts who are not cleared for adult content. */
    val adultOnly: Boolean = false,
    /**
     * A secret handshake. While it holds a non-blank value the pack is left out of the
     * lobby and out of every default deck; a host pulls it into their game by typing this
     * exact code as a line in the "Vos situations" box.
     */
    val secretCode: String? = null,
    val createdAtMillis: Long = 0,
    /**
     * The answer modes this pack may be played in. A pack offered in both modes carries
     * no restriction; narrowing it hides the pack from the lobby whenever the game runs
     * in a mode it left out. Never empty: an empty choice falls back to every mode.
     */
    val answerModes: Set<AnswerMode> = AnswerMode.entries.toSet(),
) {
    fun allows(mode: AnswerMode): Boolean = mode in answerModes

    /** True when the pack only ever joins a game through its [secretCode]. */
    val isSecret: Boolean get() = !secretCode.isNullOrBlank()
}
