package fr.ftnl.cardgame.twitch

/** One message read from a Twitch chat: which room, who typed it, and what they typed. */
data class ChatLine(val channel: String, val viewer: String, val text: String)

/**
 * Reads the chats of a set of channels until it is cancelled. Kept as an interface so a
 * test can feed lines in without a socket.
 */
fun interface TwitchChatReader {
    suspend fun read(channels: Collection<String>, onLine: suspend (ChatLine) -> Unit)
}
