package fr.ftnl.cardgame.domain.game

import kotlinx.serialization.Serializable

/** One viewer who voted from a Twitch chat, as their own chat shows them. */
@Serializable
data class ChatVoter(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
)

/**
 * What a Twitch chat gave to one answer: [count] voices — one per viewer — and the first
 * faces behind them.
 *
 * Only [MAX_FACES] viewers are kept: they are what the table actually shows under the
 * answer, the rest is a number. A chat of thousands therefore never grows the snapshot.
 */
@Serializable
data class ChatVoteTally(
    val count: Int = 0,
    val voters: List<ChatVoter> = emptyList(),
) {
    init {
        require(count >= 0) { "A chat cannot cast a negative number of votes" }
        require(voters.size <= MAX_FACES) { "At most $MAX_FACES faces are kept per answer" }
        require(voters.size <= count) { "More faces than votes" }
    }

    companion object {
        const val MAX_FACES = 15
    }
}
