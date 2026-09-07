package fr.ftnl.cardgame.domain.game

import fr.ftnl.cardgame.domain.card.PunchlineCard
import fr.ftnl.cardgame.domain.card.SituationCard
import kotlinx.serialization.Serializable

/** One card a viewer wrote, as the host reads it back. */
@Serializable
data class ChatWrittenCard(val id: String, val text: String)

/**
 * Everything the chats of this table have written, in the order it arrived.
 *
 * The piles are the wrong place to look for this: a card is drawn out of them and dealt
 * into a hand, and from there the host has no way back to it. So what the chat writes is
 * also written down here, where it only ever grows — a situation proposed in the first
 * round is still readable at the end of the game, which is what lets the host keep the
 * good ones in a deck of their own.
 */
@Serializable
data class ChatCardLog(
    val situations: List<ChatWrittenCard> = emptyList(),
    val punchlines: List<ChatWrittenCard> = emptyList(),
) {
    /** How many cards the chats have written in all, both piles together. */
    val size: Int get() = situations.size + punchlines.size

    fun record(
        situations: List<SituationCard>,
        punchlines: List<PunchlineCard>,
    ): ChatCardLog = copy(
        situations = this.situations + situations.map { ChatWrittenCard(it.id.value, it.text.raw) },
        punchlines = this.punchlines + punchlines.map { ChatWrittenCard(it.id.value, it.text) },
    )

    companion object {
        val EMPTY = ChatCardLog()
    }
}
