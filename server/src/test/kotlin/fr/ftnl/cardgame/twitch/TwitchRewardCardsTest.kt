package fr.ftnl.cardgame.twitch

import fr.ftnl.cardgame.auth.TwitchHostTokens
import fr.ftnl.cardgame.config.TwitchConfig
import fr.ftnl.cardgame.domain.engine.GameError
import fr.ftnl.cardgame.domain.game.ChatCardAccess
import fr.ftnl.cardgame.domain.game.ChatCardReward
import fr.ftnl.cardgame.domain.game.ChatCardSettings
import fr.ftnl.cardgame.domain.game.GameCode
import fr.ftnl.cardgame.domain.game.GamePhase
import fr.ftnl.cardgame.domain.game.GameSettings
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.player.Avatar
import fr.ftnl.cardgame.domain.player.AvatarPart
import fr.ftnl.cardgame.domain.player.Nickname
import fr.ftnl.cardgame.domain.player.Player
import fr.ftnl.cardgame.domain.player.PlayerId
import fr.ftnl.cardgame.game.DispatchResult
import io.ktor.client.HttpClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The channel point road, from the reward going up to the redemption being answered for.
 *
 * The point of owning the rewards is that a card can be refused honestly: a viewer who
 * paid for a card the table would not take gets their points back, which is the one thing
 * reading a chat line could never do. Twitch is faked on both sides — the channel and the
 * feed — so what is under test is the bookkeeping, and no call leaves the machine.
 */
class TwitchRewardCardsTest {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @AfterTest
    fun tearDown() = scope.cancel()

    @Test
    fun `a table on channel points puts one reward per open pile on the channel`() = runBlocking {
        val channel = FakeChannel()
        val cards = listener(channel) { updated() }

        cards.onGameCreated(game())

        channel.awaitCreated(2)
        assertEquals(
            listOf("Écrire une situation" to 500, "Écrire une réponse" to 500),
            channel.created.map { it.title to it.cost },
        )
        // Nothing is worth taking a viewer's points for until Twitch agrees to push the
        // redemptions back, so the subscriptions go up with the rewards.
        waitFor { channel.followed.size == 2 }
    }

    @Test
    fun `only the piles the host left open get a reward`() = runBlocking {
        val channel = FakeChannel()
        val rules = rules().copy(
            punchlines = false,
            situationReward = ChatCardReward(title = "Une idée", cost = 42),
        )
        val cards = listener(channel) { updated() }

        cards.onGameCreated(game(rules))

        channel.awaitCreated(1)
        assertEquals(listOf("Une idée" to 42), channel.created.map { it.title to it.cost })
    }

    @Test
    fun `a card the table took marks the redemption as done`() = runBlocking {
        val channel = FakeChannel()
        val cards = listener(channel) { updated() }

        cards.onGameCreated(game())
        channel.awaitCreated(2)
        channel.redeem(channel.created.first().id, "Le pire, c'est ____.")

        waitFor { channel.settled.isNotEmpty() }
        assertEquals(listOf("redemption-1" to true), channel.settled.toList())
    }

    @Test
    fun `a card the table refused hands the points back`() = runBlocking {
        val channel = FakeChannel()
        val cards = listener(channel) { DispatchResult.Refused(GameError.CHAT_CARDS_FULL) }

        cards.onGameCreated(game())
        channel.awaitCreated(2)
        channel.redeem(channel.created.first().id, "une carte de trop")

        waitFor { channel.settled.isNotEmpty() }
        assertEquals(listOf("redemption-1" to false), channel.settled.toList())
    }

    @Test
    fun `an empty box never reaches the table, and is refunded all the same`() = runBlocking {
        val channel = FakeChannel()
        val dispatched = AtomicInteger()
        val cards = listener(channel) {
            dispatched.incrementAndGet()
            updated()
        }

        cards.onGameCreated(game())
        channel.awaitCreated(2)
        channel.redeem(channel.created.first().id, "  ")

        waitFor { channel.settled.isNotEmpty() }
        assertEquals(listOf("redemption-1" to false), channel.settled.toList())
        assertEquals(0, dispatched.get())
    }

    @Test
    fun `a reward the host pointed at is adopted rather than built again`() = runBlocking {
        val channel = FakeChannel()
        channel.put(StandingReward("reward-mine", "Ma récompense", 1_000), managed = true)
        val cards = listener(channel) { updated() }

        cards.onGameCreated(game(adopting("reward-mine")))

        waitFor { channel.followed.contains("reward-mine") }
        assertTrue(channel.created.isEmpty(), "nothing should have been created")
    }

    @Test
    fun `an adopted reward is settled when the game owns it`() = runBlocking {
        val channel = FakeChannel()
        channel.put(StandingReward("reward-mine", "Ma récompense", 1_000), managed = true)
        val cards = listener(channel) { updated() }

        cards.onGameCreated(game(adopting("reward-mine")))
        waitFor { channel.followed.contains("reward-mine") }
        channel.redeem("reward-mine", "Le pire, c'est ____.")

        waitFor { channel.settled.isNotEmpty() }
        assertEquals(listOf("redemption-1" to true), channel.settled.toList())
    }

    // Twitch only lets the application that created a reward fulfil or cancel it. A reward
    // the streamer built themselves is still readable — the card reaches the table — but
    // its redemption stays in their queue, which is what the lobby warns about.
    @Test
    fun `a reward the game does not own takes the card and leaves the redemption alone`() =
        runBlocking {
            val channel = FakeChannel()
            channel.put(StandingReward("reward-theirs", "La leur", 1_000), managed = false)
            val taken = AtomicInteger()
            val cards = listener(channel) {
                taken.incrementAndGet()
                updated()
            }

            cards.onGameCreated(game(adopting("reward-theirs")))
            waitFor { channel.followed.contains("reward-theirs") }
            channel.redeem("reward-theirs", "Le pire, c'est ____.")

            waitFor { taken.get() == 1 }
            delay(SETTLE_MILLIS)
            assertTrue(channel.settled.isEmpty(), "it is not ours to settle")
        }

    @Test
    fun `an adopted reward is left standing when the table breaks up`() = runBlocking {
        val channel = FakeChannel()
        channel.put(StandingReward("reward-mine", "Ma récompense", 1_000), managed = true)
        val cards = listener(channel) { updated() }

        cards.onGameCreated(game(adopting("reward-mine")))
        waitFor { channel.followed.contains("reward-mine") }
        cards.onGameForgotten(CODE)

        delay(SETTLE_MILLIS)
        assertTrue(channel.deleted.isEmpty(), "the host had it before this table")
    }

    @Test
    fun `the rewards come down with the table`() = runBlocking {
        val channel = FakeChannel()
        val cards = listener(channel) { updated() }

        cards.onGameCreated(game())
        channel.awaitCreated(2)
        cards.onGameForgotten(CODE)

        waitFor { channel.deleted.size == 2 }
        assertEquals(channel.created.map { it.id }.toSet(), channel.deleted.toSet())
        assertEquals(0, cards.size)
    }

    @Test
    fun `a host who never opened their channel points has nothing put on their channel`() =
        runBlocking {
            val channel = FakeChannel()
            val cards = TwitchRewardCards(channel, channel, TwitchHostTokens(HttpClient(), config()), scope) { _, _ ->
                updated()
            }

            cards.onGameCreated(game())

            delay(SETTLE_MILLIS)
            assertTrue(channel.created.isEmpty())
        }

    @Test
    fun `every other way of paying leaves the channel alone`() = runBlocking {
        val channel = FakeChannel()
        val cards = listener(channel) { updated() }

        cards.onGameCreated(game(rules().copy(access = ChatCardAccess.EVERYONE)))
        cards.onGameCreated(game(rules().copy(access = ChatCardAccess.BITS)))

        delay(SETTLE_MILLIS)
        assertTrue(channel.created.isEmpty())
    }

    // --- the props ----------------------------------------------------------------------

    private fun listener(channel: FakeChannel, dispatch: suspend () -> DispatchResult) =
        TwitchRewardCards(channel, channel, consented(), scope) { _, _ -> dispatch() }

    /** A host who has granted the game the right to own rewards on their channel. */
    private fun consented() = TwitchHostTokens(HttpClient(), config()).apply {
        remember(HOST_TWITCH_ID, "host-token", refreshToken = null, expiresInSeconds = 3_600)
    }

    private fun config() = TwitchConfig("client", "secret", "http://localhost/auth/twitch/callback")

    private fun updated(): DispatchResult = DispatchResult.Updated(game(), emptyList())

    private fun rules() = ChatCardSettings(access = ChatCardAccess.CHANNEL_POINTS)

    /** Both piles pointed at one reward already on the channel. */
    private fun adopting(rewardId: String) = rules().copy(
        punchlines = false,
        situationReward = ChatCardReward(id = rewardId),
    )

    private fun game(chatCards: ChatCardSettings = rules()) = GameState(
        code = CODE,
        hostId = HOST,
        players = listOf(
            Player(
                id = HOST,
                nickname = Nickname.of("Kameto"),
                avatar = Avatar(AvatarPart("head-1", "#ff8800"), AvatarPart("body-1", "#3355ff")),
                twitchLogin = "kameto",
                twitchId = HOST_TWITCH_ID,
            ),
        ),
        settings = GameSettings(chatCards = chatCards),
        phase = GamePhase.LOBBY,
    )

    private suspend fun waitFor(condition: () -> Boolean) = withTimeout(TIMEOUT) {
        while (!condition()) delay(10)
    }

    /**
     * The channel as Twitch would hold it, and the feed it pushes redemptions down: one
     * object, because a test that redeems a reward wants the id the channel just handed
     * out. The feed stays open, exactly as a socket does, so a redemption can land in the
     * middle of a test.
     */
    private class FakeChannel : TwitchRewards, TwitchEventFeed {
        data class Made(val id: String, val title: String, val cost: Int)

        val created = CopyOnWriteArrayList<Made>()
        val deleted = ConcurrentHashMap.newKeySet<String>()
        val followed = ConcurrentHashMap.newKeySet<String>()
        val settled = CopyOnWriteArrayList<Pair<String, Boolean>>()

        /** Rewards that were on the channel before this table sat down. */
        val standing = CopyOnWriteArrayList<StandingReward>()

        /** Of those, the ones the game created and may therefore settle. */
        val manageable = ConcurrentHashMap.newKeySet<String>()

        override suspend fun list(
            token: String,
            broadcasterId: String,
            onlyManageable: Boolean,
        ): List<StandingReward> {
            val ours = created.map { StandingReward(it.id, it.title, it.cost) } +
                standing.filter { it.id in manageable }
            return if (onlyManageable) ours else standing + created.map {
                StandingReward(it.id, it.title, it.cost)
            }
        }

        fun put(reward: StandingReward, managed: Boolean) {
            standing += reward
            if (managed) manageable += reward.id
        }

        private val opened = CompletableDeferred<Unit>()
        private var onRedemption: (suspend (RewardRedemption) -> Unit)? = null
        private val redemptions = AtomicInteger()

        override suspend fun create(
            token: String,
            broadcasterId: String,
            title: String,
            cost: Int,
            prompt: String,
        ): String = "reward-${created.size + 1}".also { created += Made(it, title, cost) }

        override suspend fun delete(token: String, broadcasterId: String, rewardId: String): Boolean =
            deleted.add(rewardId)

        override suspend fun settle(
            token: String,
            broadcasterId: String,
            rewardId: String,
            redemptionId: String,
            fulfilled: Boolean,
        ): Boolean {
            settled += redemptionId to fulfilled
            return true
        }

        override suspend fun follow(
            token: String,
            broadcasterId: String,
            rewardId: String,
            sessionId: String,
        ): Boolean = followed.add(rewardId)

        override suspend fun listen(
            onReady: suspend (String) -> Boolean,
            onRedemption: suspend (RewardRedemption) -> Unit,
        ) {
            this.onRedemption = onRedemption
            onReady("session-1")
            opened.complete(Unit)
            // A socket stays open until somebody hangs up; so does this.
            CompletableDeferred<Unit>().await()
        }

        suspend fun awaitCreated(count: Int) = withTimeout(TIMEOUT) {
            opened.await()
            while (created.size < count) delay(10)
        }

        suspend fun redeem(rewardId: String, text: String) {
            onRedemption?.invoke(
                RewardRedemption(
                    rewardId = rewardId,
                    redemptionId = "redemption-${redemptions.incrementAndGet()}",
                    viewerId = "42",
                    viewerName = "Spectateur",
                    text = text,
                )
            )
        }
    }

    private companion object {
        val CODE = GameCode.of("ABCDE")
        val HOST = PlayerId("host")
        const val HOST_TWITCH_ID = "1234"
        const val TIMEOUT = 5_000L

        /** Long enough for a listener that was going to act to have acted. */
        const val SETTLE_MILLIS = 200L
    }
}
