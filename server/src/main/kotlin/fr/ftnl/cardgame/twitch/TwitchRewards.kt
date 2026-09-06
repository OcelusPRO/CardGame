package fr.ftnl.cardgame.twitch

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

/** One reward already standing on a channel, as the lobby needs to offer it. */
data class StandingReward(val id: String, val title: String, val cost: Int)

/**
 * The channel point rewards the game owns, on a host's own channel.
 *
 * Every call rides the host's token: Twitch only lets an application touch the rewards it
 * created itself, which is exactly the fence wanted here — the game can never rename,
 * price or delete a reward the streamer set up for something else.
 *
 * Kept as an interface so a test can watch what a table asks of a channel without a single
 * call leaving the machine, exactly like [TwitchChatReader] does for the chat.
 */
interface TwitchRewards {

    /**
     * What is already standing on the channel. [onlyManageable] narrows it to the rewards
     * this application created, which is the only set it may follow, rename or remove —
     * Twitch reserves all three to the client id that made the reward.
     *
     * Null when Twitch refused the question altogether: a channel that is neither
     * affiliate nor partner has no custom rewards to speak of, and says so with a 403.
     */
    suspend fun list(token: String, broadcasterId: String, onlyManageable: Boolean): List<StandingReward>?

    /** Creates one reward, and answers with the id its redemptions will carry. */
    suspend fun create(
        token: String,
        broadcasterId: String,
        title: String,
        cost: Int,
        prompt: String,
    ): String?

    /** Takes the reward back off the channel. Only ours can be deleted, which is the point. */
    suspend fun delete(token: String, broadcasterId: String, rewardId: String): Boolean

    /**
     * Settles one redemption: kept when the card made it onto the table, cancelled when it
     * did not — and cancelling is what gives the viewer their points back.
     */
    suspend fun settle(
        token: String,
        broadcasterId: String,
        rewardId: String,
        redemptionId: String,
        fulfilled: Boolean,
    ): Boolean

    /**
     * Asks Twitch to push the redemptions of one reward down an open socket. The session
     * id comes from the welcome frame, and the subscription dies with the socket.
     */
    suspend fun follow(token: String, broadcasterId: String, rewardId: String, sessionId: String): Boolean
}

/**
 * The Helix side of it.
 *
 * A failure is never fatal. A streamer who is not an affiliate cannot have custom rewards
 * at all, and Twitch says so with a 403; the mode then simply does not open, and the rest
 * of the game carries on.
 */
class HelixRewards(private val http: HttpClient, private val clientId: String) : TwitchRewards {

    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun list(
        token: String,
        broadcasterId: String,
        onlyManageable: Boolean,
    ): List<StandingReward>? = call("list the rewards") {
        val response: HttpResponse = http.get(REWARDS_URL) {
            auth(token)
            parameter("broadcaster_id", broadcasterId)
            parameter("only_manageable_rewards", onlyManageable)
        }
        if (!response.status.isSuccess()) return@call refused(response, "listing the rewards")
        response.body<Rewards>().data.map { StandingReward(it.id, it.title, it.cost) }
    }

    override suspend fun create(
        token: String,
        broadcasterId: String,
        title: String,
        cost: Int,
        prompt: String,
    ): String? = call("create the reward \"$title\"") {
        val response: HttpResponse = http.post(REWARDS_URL) {
            auth(token)
            parameter("broadcaster_id", broadcasterId)
            contentType(ContentType.Application.Json)
            setBody(
                NewReward(
                    title = title,
                    cost = cost,
                    prompt = prompt,
                )
            )
        }
        if (!response.status.isSuccess()) return@call refused(response, "creating a reward")
        response.body<Rewards>().data.firstOrNull()?.id
    }

    override suspend fun delete(token: String, broadcasterId: String, rewardId: String): Boolean =
        call("delete a reward") {
            val response: HttpResponse = http.delete(REWARDS_URL) {
                auth(token)
                parameter("broadcaster_id", broadcasterId)
                parameter("id", rewardId)
            }
            response.status.isSuccess()
        } ?: false

    override suspend fun settle(
        token: String,
        broadcasterId: String,
        rewardId: String,
        redemptionId: String,
        fulfilled: Boolean,
    ): Boolean = call("settle a redemption") {
        val response: HttpResponse = http.patch(REDEMPTIONS_URL) {
            auth(token)
            parameter("broadcaster_id", broadcasterId)
            parameter("reward_id", rewardId)
            parameter("id", redemptionId)
            contentType(ContentType.Application.Json)
            setBody(RedemptionStatus(if (fulfilled) "FULFILLED" else "CANCELED"))
        }
        if (!response.status.isSuccess()) return@call refused(response, "settling a redemption")
        true
    } ?: false

    override suspend fun follow(
        token: String,
        broadcasterId: String,
        rewardId: String,
        sessionId: String,
    ): Boolean = call("follow a reward") {
        val response: HttpResponse = http.post(SUBSCRIPTIONS_URL) {
            auth(token)
            contentType(ContentType.Application.Json)
            setBody(
                Subscription(
                    condition = Condition(broadcasterId, rewardId),
                    transport = Transport(sessionId = sessionId),
                )
            )
        }
        if (!response.status.isSuccess()) return@call refused(response, "following a reward")
        true
    } ?: false

    private fun io.ktor.client.request.HttpRequestBuilder.auth(token: String) {
        bearerAuth(token)
        header("Client-Id", clientId)
    }

    private suspend fun <T> call(what: String, block: suspend () -> T?): T? = runCatching { block() }
        .onFailure { log.warn("Twitch could not {}", what, it) }
        .getOrNull()

    /** Twitch says why in the body, and a 403 on an affiliate rule is worth reading. */
    private suspend fun refused(response: HttpResponse, what: String): Nothing? {
        log.warn("Twitch refused {} ({}): {}", what, response.status, response.bodyAsText())
        return null
    }

    @Serializable
    private data class NewReward(
        val title: String,
        val cost: Int,
        val prompt: String,
        /** The card itself: without this there is no box for the viewer to write in. */
        @SerialName("is_user_input_required") val userInput: Boolean = true,
        /**
         * Redemptions land in the queue rather than counting as done on the spot, which
         * is the only way a refused card can be cancelled and the points handed back.
         */
        @SerialName("should_redemptions_skip_request_queue") val skipQueue: Boolean = false,
    )

    @Serializable
    private data class Rewards(val data: List<Reward> = emptyList())

    @Serializable
    private data class Reward(val id: String, val title: String = "", val cost: Int = 0)

    @Serializable
    private data class RedemptionStatus(val status: String)

    @Serializable
    private data class Subscription(
        val condition: Condition,
        val transport: Transport,
        val type: String = REDEMPTION_TYPE,
        val version: String = "1",
    )

    @Serializable
    private data class Condition(
        @SerialName("broadcaster_user_id") val broadcasterUserId: String,
        @SerialName("reward_id") val rewardId: String,
    )

    @Serializable
    private data class Transport(
        val method: String = "websocket",
        @SerialName("session_id") val sessionId: String,
    )

    private companion object {
        const val REWARDS_URL = "https://api.twitch.tv/helix/channel_points/custom_rewards"
        const val REDEMPTIONS_URL = "https://api.twitch.tv/helix/channel_points/custom_rewards/redemptions"
        const val SUBSCRIPTIONS_URL = "https://api.twitch.tv/helix/eventsub/subscriptions"
        const val REDEMPTION_TYPE = "channel.channel_points_custom_reward_redemption.add"
    }
}
