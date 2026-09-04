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
 */
class TwitchChatSocket(
    private val http: HttpClient,
    private val url: String,
) : TwitchChatReader {

    override suspend fun read(channels: Collection<String>, onLine: suspend (ChatLine) -> Unit) {
        val rooms = channels.map { it.lowercase() }.filter { it.isNotBlank() }.distinct()
        if (rooms.isEmpty()) return
        http.webSocket(url) {
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

    /** `:viewer!viewer@viewer.tmi.twitch.tv PRIVMSG #channel :the message` */
    private fun privmsg(line: String): ChatLine? {
        if (!line.startsWith(":")) return null
        val space = line.indexOf(' ').takeIf { it > 0 } ?: return null
        val viewer = line.substring(1, space).substringBefore('!').lowercase()
        val rest = line.substring(space + 1)
        if (!rest.startsWith("PRIVMSG #")) return null
        val channel = rest.removePrefix("PRIVMSG #").substringBefore(' ')
        val message = rest.substringAfter(" :", missingDelimiterValue = "")
        return if (channel.isBlank() || message.isBlank()) null
        else ChatLine(channel = channel, viewer = viewer, text = message)
    }
}
