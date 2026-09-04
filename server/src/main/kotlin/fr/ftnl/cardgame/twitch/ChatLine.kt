package fr.ftnl.cardgame.twitch

/**
 * One message read from a Twitch chat: which room, who typed it, and what they typed.
 *
 * [viewerId] is the numeric account id, the only stable handle a viewer has — a display
 * name can change, and two chats can hold the same nickname. It is what tells one voice
 * from another, and what a profile picture is looked up with.
 */
data class ChatLine(
    val channel: String,
    val viewerId: String,
    val viewerName: String,
    val text: String,
)

/**
 * Reads the chats of a set of channels until it is cancelled. Kept as an interface so a
 * test can feed lines in without a socket.
 */
fun interface TwitchChatReader {
    suspend fun read(channels: Collection<String>, onLine: suspend (ChatLine) -> Unit)
}

/**
 * Profile pictures of the viewers who voted, by account id. Missing ones are simply
 * absent: a face nobody could resolve is drawn from the name instead.
 */
fun interface ViewerPictures {
    suspend fun of(ids: Collection<String>): Map<String, String>

    companion object {
        /** What the game uses when Twitch is not configured: names, and no faces. */
        val NONE = ViewerPictures { emptyMap() }
    }
}
