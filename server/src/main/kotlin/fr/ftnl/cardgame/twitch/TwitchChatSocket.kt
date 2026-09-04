package fr.ftnl.cardgame.twitch

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.channels.consumeEach
import kotlin.random.Random

/**
 * Reads Twitch chats over the IRC gateway, anonymously: signing in as `justinfan<n>` is
 * how Twitch lets anyone listen to a public room. No token is involved, so a player never
 * grants this application anything beyond reading their profile — and the server can
 * never write a single word in their chat.
 *
 * The `tags` capability is asked for because a bare IRC line only carries a nickname:
 * the tags add the account id, which is what makes one voice one viewer, and the display
 * name the chat itself shows.
 */
class TwitchChatSocket(
    private val http: HttpClient,
    private val url: String,
) : TwitchChatReader {

    override suspend fun read(channels: Collection<String>, onLine: suspend (ChatLine) -> Unit) {
        val rooms = channels.map { it.lowercase() }.filter { it.isNotBlank() }.distinct()
        if (rooms.isEmpty()) return
        http.webSocket(url) {
            send("CAP REQ :twitch.tv/tags")
            send("NICK justinfan${Random.nextInt(10_000, 99_999)}")
            rooms.forEach { send("JOIN #$it") }
            incoming.consumeEach { frame ->
                val text = (frame as? Frame.Text)?.readText() ?: return@consumeEach
                text.split("\r\n").filter { it.isNotBlank() }.forEach { line ->
                    // The gateway drops a client that stops answering its keep-alive.
                    if (line.startsWith("PING")) send("PONG :tmi.twitch.tv")
                    else privmsg(line)?.let { onLine(it) }
                }
            }
        }
    }

    /**
     * `@display-name=Foo;user-id=42;… :foo!foo@foo.tmi.twitch.tv PRIVMSG #channel :hello`
     *
     * The tag block is optional: should Twitch ever answer without it, the nickname
     * stands in for both the id and the name rather than dropping the vote.
     */
    private fun privmsg(line: String): ChatLine? {
        val tags = if (line.startsWith("@")) tagsOf(line.substringBefore(' ')) else emptyMap()
        val rest = if (line.startsWith("@")) line.substringAfter(' ') else line
        if (!rest.startsWith(":")) return null
        val space = rest.indexOf(' ').takeIf { it > 0 } ?: return null
        val nick = rest.substring(1, space).substringBefore('!').lowercase()
        val command = rest.substring(space + 1)
        if (!command.startsWith("PRIVMSG #")) return null
        val channel = command.removePrefix("PRIVMSG #").substringBefore(' ')
        val message = command.substringAfter(" :", missingDelimiterValue = "")
        if (channel.isBlank() || message.isBlank() || nick.isBlank()) return null
        return ChatLine(
            channel = channel,
            viewerId = tags["user-id"]?.takeIf { it.isNotBlank() } ?: nick,
            viewerName = tags["display-name"]?.takeIf { it.isNotBlank() } ?: nick,
            text = message,
        )
    }

    private fun tagsOf(block: String): Map<String, String> =
        block.removePrefix("@").split(';').mapNotNull { tag ->
            val name = tag.substringBefore('=')
            val value = tag.substringAfter('=', missingDelimiterValue = "")
            if (name.isBlank()) null else name to value
        }.toMap()
}
