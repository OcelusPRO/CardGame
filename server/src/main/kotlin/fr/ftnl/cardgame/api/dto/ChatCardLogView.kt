package fr.ftnl.cardgame.api.dto

import kotlinx.serialization.Serializable

/** One card a viewer wrote, as it appears in the host's own card boxes. */
@Serializable
data class ChatWrittenCardView(val id: String, val text: String)

/**
 * Everything the chats have written on this table, for the host to keep or throw away.
 *
 * Only ever filled for the host: they are the one composing the paquet, and a proposal
 * they end up refusing has no business showing on anybody else's screen.
 */
@Serializable
data class ChatCardLogView(
    val situations: List<ChatWrittenCardView> = emptyList(),
    val punchlines: List<ChatWrittenCardView> = emptyList(),
)
