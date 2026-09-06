package fr.ftnl.cardgame.twitch

import fr.ftnl.cardgame.auth.TwitchHostTokens
import fr.ftnl.cardgame.domain.card.CardId
import fr.ftnl.cardgame.domain.card.CardOrigin
import fr.ftnl.cardgame.domain.card.PunchlineCard
import fr.ftnl.cardgame.domain.card.SituationCard
import fr.ftnl.cardgame.domain.card.SituationText
import fr.ftnl.cardgame.domain.engine.GameCommand
import fr.ftnl.cardgame.domain.engine.GameEvent
import fr.ftnl.cardgame.domain.game.ChatCardReward
import fr.ftnl.cardgame.domain.game.GameCode
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.game.DispatchResult
import fr.ftnl.cardgame.game.GameListener
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * The channel point way in: the game puts the rewards up, or adopts them, and follows what
 * is redeemed.
 *
 * A redemption is not a chat line. It arrives through EventSub with the card in its own
 * box, which is what lets this settle it honestly: the card that reached the table is
 * marked as done, and the one that did not — too short, too late, past the ceiling — is
 * cancelled, and cancelling is what gives the viewer their points back.
 *
 * Settling is the one thing Twitch fences off. **Only the application that created a reward
 * may fulfil or cancel its redemptions**, so a reward the streamer made in their dashboard
 * can be *read* here but never answered for: those redemptions sit in the streamer's own
 * queue, and they validate them by hand. The lobby says so before the host chooses, which
 * is the whole reason [Pile.Adopt] and [Pile.Create] are different things.
 *
 * A reward the game put up lives exactly as long as the table needs it: up when the host
 * picks the mode, down when the game ends. An adopted one is left exactly where it stood.
 */
class TwitchRewardCards(
    private val rewards: TwitchRewards,
    private val feed: TwitchEventFeed,
    private val tokens: TwitchHostTokens,
    private val scope: CoroutineScope,
    private val dispatch: suspend (GameCode, GameCommand) -> DispatchResult,
) : GameListener {

    private val log = LoggerFactory.getLogger(javaClass)
    private val watched = ConcurrentHashMap<String, Watch>()

    override suspend fun onGameCreated(state: GameState) = follow(state)

    override suspend fun onGameChanged(state: GameState, events: List<GameEvent>) = follow(state)

    override suspend fun onGameForgotten(code: GameCode) = close(code)

    val size: Int get() = watched.size

    private fun follow(state: GameState) {
        val terms = termsOf(state)
        if (terms == null) return close(state.code)
        synchronized(watched) {
            if (watched[state.code.value]?.terms == terms) return
            watched.remove(state.code.value)?.job?.cancel()
            val job = scope.launch { run(state.code, terms) }
            watched[state.code.value] = Watch(terms, job)
        }
    }

    private fun close(code: GameCode) {
        synchronized(watched) { watched.remove(code.value)?.job?.cancel() }
    }

    /**
     * Gives up every reward standing on one channel, and waits for them to actually come
     * down. Called before the consent is withdrawn, while there is still a token to take
     * them down with.
     */
    suspend fun release(broadcasterId: String) {
        val jobs = synchronized(watched) {
            watched.entries
                .filter { it.value.terms.broadcasterId == broadcasterId }
                .map { it.key to it.value.job }
                .onEach { (code, _) -> watched.remove(code) }
                .map { (_, job) -> job }
        }
        jobs.forEach { it.cancelAndJoin() }
    }

    /**
     * What this table asks of the channel, and nothing else: the rewards go back up only
     * when one of these actually changed. A host nudging the round timer does not make
     * their viewers watch a reward disappear and come back.
     */
    private fun termsOf(state: GameState): Terms? {
        val channelId = state.rewardChannelId ?: return null
        if (!tokens.holds(channelId)) return null
        val rules = state.settings.chatCards
        return Terms(
            broadcasterId = channelId,
            situation = pileOf(rules.situationReward.takeIf { rules.situations }, ChatCardReward.SITUATION),
            punchline = pileOf(rules.punchlineReward.takeIf { rules.punchlines }, ChatCardReward.PUNCHLINE),
        )
    }

    /**
     * How one pile gets its reward. A reward the host pointed at is adopted by its id
     * alone: its name and its price are Twitch's copy of them, and rewriting those here
     * would be renaming somebody's existing reward behind their back.
     */
    private fun pileOf(reward: ChatCardReward?, fallback: ChatCardReward): Pile? = when {
        reward == null -> null
        !reward.isNew -> Pile.Adopt(reward.id)
        // A host who emptied the name box gets the default one back rather than a reward
        // Twitch would refuse for having no title at all.
        else -> Pile.Create(reward.title.ifBlank { fallback.title }, reward.cost)
    }

    private suspend fun run(code: GameCode, terms: Terms) {
        val standing = raise(terms)
        if (standing.kinds.isEmpty()) {
            log.warn("No channel point reward could be raised for {}", terms.broadcasterId)
            return
        }
        try {
            while (currentCoroutineContext().isActive) {
                try {
                    feed.listen(
                        onReady = { session -> subscribe(terms, standing.kinds.keys, session) },
                        onRedemption = { redemption -> settle(code, terms, standing, redemption) },
                    )
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    log.warn("The EventSub feed of {} dropped, retrying", terms.broadcasterId, failure)
                }
                delay(RETRY_MILLIS)
            }
        } finally {
            // The socket is gone either way, and the rewards the game put up must not
            // outlive it — a cancelled job still owes the channel that cleanup. An adopted
            // reward is left alone: the host had it before this table, and keeps it after.
            withContext(NonCancellable) { remove(terms, standing.created) }
        }
    }

    /** The rewards this table will read, by the id their redemptions carry. */
    private suspend fun raise(terms: Terms): Standing {
        val token = tokens.token(terms.broadcasterId) ?: return Standing()
        val kinds = mutableMapOf<String, ChatCardKind>()
        val created = mutableSetOf<String>()
        suspend fun raise(pile: Pile?, kind: ChatCardKind, prompt: String) {
            when (pile) {
                null -> Unit
                is Pile.Adopt -> kinds[pile.rewardId] = kind
                is Pile.Create ->
                    rewards.create(token, terms.broadcasterId, pile.title, pile.cost, prompt)?.let {
                        kinds[it] = kind
                        created += it
                    }
            }
        }
        raise(terms.situation, ChatCardKind.SITUATION, SITUATION_PROMPT)
        raise(terms.punchline, ChatCardKind.PUNCHLINE, PUNCHLINE_PROMPT)
        return Standing(kinds, created, settleable(terms, token, kinds.keys, created))
    }

    /**
     * Which of these redemptions the game may answer for.
     *
     * Everything it created, plus whatever Twitch still hands back as manageable — an
     * adopted reward the game made for an earlier table qualifies, one the streamer built
     * in their dashboard never will. Asked rather than assumed, because it is the streamer
     * who pays for a wrong guess: a redemption nobody may settle stays in their queue.
     */
    private suspend fun settleable(
        terms: Terms,
        token: String,
        all: Set<String>,
        created: Set<String>,
    ): Set<String> {
        val adopted = all - created
        if (adopted.isEmpty()) return created
        val ours = rewards.list(token, terms.broadcasterId, onlyManageable = true)
            ?.map { it.id }
            ?.toSet()
            ?: return created
        val theirs = adopted - ours
        if (theirs.isNotEmpty()) {
            log.info(
                "{} keeps {} reward(s) the game may read but not settle; their redemptions are theirs to validate",
                terms.broadcasterId,
                theirs.size,
            )
        }
        return created + (adopted intersect ours)
    }

    private suspend fun subscribe(terms: Terms, rewardIds: Set<String>, sessionId: String): Boolean {
        val token = tokens.token(terms.broadcasterId) ?: return false
        // All of them, or none: half a feed would take a viewer's points for a pile
        // nothing is listening to. An adopted reward the host has since deleted fails
        // right here, and says so rather than going quiet.
        return rewardIds.all { rewardId ->
            rewards.follow(token, terms.broadcasterId, rewardId, sessionId).also { followed ->
                if (!followed) log.warn("Twitch would not hand over the redemptions of {}", rewardId)
            }
        }
    }

    private suspend fun remove(terms: Terms, created: Set<String>) {
        if (created.isEmpty()) return
        val token = tokens.token(terms.broadcasterId) ?: return
        created.forEach { rewards.delete(token, terms.broadcasterId, it) }
    }

    /**
     * One redemption, taken to the table and then answered for where the game may.
     *
     * On a reward it owns, a card that did not make it is cancelled rather than quietly
     * kept: the viewer paid, and got nothing. On a reward it merely reads, there is
     * nothing to say — Twitch only listens to the application that created it — so the
     * redemption stays in the streamer's queue, which is what the lobby warned about.
     */
    private suspend fun settle(
        code: GameCode,
        terms: Terms,
        standing: Standing,
        redemption: RewardRedemption,
    ) {
        val kind = standing.kinds[redemption.rewardId] ?: return
        val accepted = accept(code, kind, redemption.text)
        if (redemption.rewardId !in standing.settleable) return
        val token = tokens.token(terms.broadcasterId) ?: return
        rewards.settle(
            token = token,
            broadcasterId = terms.broadcasterId,
            rewardId = redemption.rewardId,
            redemptionId = redemption.redemptionId,
            fulfilled = accepted,
        )
    }

    private suspend fun accept(code: GameCode, kind: ChatCardKind, written: String): Boolean {
        val text = ChatCardCommand.cardOf(written) ?: return false
        return dispatch(code, command(kind, text)) is DispatchResult.Updated
    }

    /** A card written by a chat is a card of this game and of no other, hence a throwaway id. */
    private fun command(kind: ChatCardKind, text: String): GameCommand = when (kind) {
        ChatCardKind.SITUATION -> GameCommand.AddChatCards(
            situations = listOf(
                SituationCard(CardId("chat-s-${UUID.randomUUID()}"), SituationText(text), CardOrigin.CHAT),
            ),
        )
        ChatCardKind.PUNCHLINE -> GameCommand.AddChatCards(
            punchlines = listOf(
                PunchlineCard(CardId("chat-p-${UUID.randomUUID()}"), text, CardOrigin.CHAT),
            ),
        )
    }

    private class Watch(val terms: Terms, val job: Job)

    /** What is on the channel for this table, what the game put there, and what it may settle. */
    private class Standing(
        val kinds: Map<String, ChatCardKind> = emptyMap(),
        val created: Set<String> = emptySet(),
        val settleable: Set<String> = emptySet(),
    )

    /** How one pile gets its reward: an existing one, or one the game puts up itself. */
    private sealed interface Pile {
        /** A reward already on the channel, followed where it stands and left there. */
        data class Adopt(val rewardId: String) : Pile

        data class Create(val title: String, val cost: Int) : Pile
    }

    private data class Terms(
        val broadcasterId: String,
        val situation: Pile?,
        val punchline: Pile?,
    )

    private companion object {
        const val RETRY_MILLIS = 5_000L
        const val SITUATION_PROMPT =
            "Écrivez une situation à trous. Les ____ marquent ce que les joueurs rempliront."
        const val PUNCHLINE_PROMPT = "Écrivez une réponse : courte, et qui claque."
    }
}
