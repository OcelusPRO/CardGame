package fr.ftnl.cardgame.api.dto

import kotlinx.serialization.Serializable

/**
 * An answer on the table. [authorId] and [votes] stay null while the round is being
 * judged, so nobody can tell who wrote what before the reveal — [chatVotes], on the other
 * hand, is live: it is the whole point of a chat voting.
 */
@Serializable
data class AnswerView(
    val id: Int,
    val texts: List<String>,
    val filledText: String,
    val authorId: String? = null,
    val votes: Int? = null,
    val isMine: Boolean = false,
    val chatVotes: ChatVotesView? = null,
)

/** What the Twitch chats gave an answer: a number of voices, and the first faces. */
@Serializable
data class ChatVotesView(
    val count: Int,
    val voters: List<ChatVoterView> = emptyList(),
)

/** One viewer who voted, as their chat shows them. */
@Serializable
data class ChatVoterView(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
)
