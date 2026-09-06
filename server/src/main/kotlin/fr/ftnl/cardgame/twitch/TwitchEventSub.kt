package fr.ftnl.cardgame.twitch

import fr.ftnl.cardgame.plugins.ApiJson
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

/** One channel point reward redeemed, with whatever the viewer typed in its box. */
data class RewardRedemption(
    val rewardId: String,
    val redemptionId: String,
    val viewerId: String,
    val viewerName: String,
    val text: String,
)

/**
 * The redemptions of a set of rewards, as they happen. Kept as an interface so a test can
 * feed events in without a socket, exactly like [TwitchChatReader] does for the chat.
 *
 * [onReady] is handed the session id of a freshly opened socket, and answers whether the
 * subscriptions could be made: Twitch wants them created *on* an open socket, and a socket
 * nobody subscribed anything to is only going to time out.
 */
fun interface TwitchEventFeed {
    suspend fun listen(
        onReady: suspend (sessionId: String) -> Boolean,
        onRedemption: suspend (RewardRedemption) -> Unit,
    )
}

/**
 * EventSub over a WebSocket rather than a webhook: no public callback URL to expose, no
 * signature to verify, and a subscription that dies with the socket — which is what a
 * reward living exactly as long as one game wants.
 *
 * Twitch hands out a session id in its welcome frame, keeps the socket warm with
 * keepalives, and asks for a move to a new address when it wants to recycle this one.
 * Returning on that is enough: whoever is listening reopens, and the caller's retry loop
 * is the same one that covers a dropped connection.
 */
class TwitchEventSubSocket(
    private val http: HttpClient,
    private val url: String = DEFAULT_URL,
) : TwitchEventFeed {

    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun listen(
        onReady: suspend (String) -> Boolean,
        onRedemption: suspend (RewardRedemption) -> Unit,
    ) {
        http.webSocket(url) {
            incoming.consumeEach { frame ->
                val text = (frame as? Frame.Text)?.readText() ?: return@consumeEach
                val message = read(text) ?: return@consumeEach
                when (message.metadata.type) {
                    WELCOME -> {
                        val session = message.payload?.session?.id ?: return@consumeEach
                        if (!onReady(session)) return@webSocket
                    }
                    NOTIFICATION -> message.payload?.event?.let { onRedemption(it.asRedemption()) }
                    // Twitch is recycling the socket, or has taken a subscription away.
                    // Either way this session is over; the caller opens the next one.
                    RECONNECT -> return@webSocket
                    REVOCATION -> {
                        log.warn("Twitch revoked an EventSub subscription")
                        return@webSocket
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun read(text: String): Message? = runCatching { ApiJson.decodeFromString<Message>(text) }
        .onFailure { log.debug("Unreadable EventSub frame", it) }
        .getOrNull()

    @Serializable
    private data class Message(val metadata: Metadata, val payload: Payload? = null)

    @Serializable
    private data class Metadata(@SerialName("message_type") val type: String)

    @Serializable
    private data class Payload(val session: Session? = null, val event: Event? = null)

    @Serializable
    private data class Session(val id: String)

    @Serializable
    private data class Event(
        val id: String,
        @SerialName("user_id") val userId: String = "",
        @SerialName("user_name") val userName: String = "",
        @SerialName("user_login") val userLogin: String = "",
        @SerialName("user_input") val userInput: String = "",
        val reward: Reward = Reward(),
    ) {
        fun asRedemption() = RewardRedemption(
            rewardId = reward.id,
            redemptionId = id,
            viewerId = userId,
            viewerName = userName.ifBlank { userLogin },
            text = userInput,
        )
    }

    @Serializable
    private data class Reward(val id: String = "")

    private companion object {
        const val DEFAULT_URL = "wss://eventsub.wss.twitch.tv/ws"
        const val WELCOME = "session_welcome"
        const val NOTIFICATION = "notification"
        const val RECONNECT = "session_reconnect"
        const val REVOCATION = "revocation"
    }
}
