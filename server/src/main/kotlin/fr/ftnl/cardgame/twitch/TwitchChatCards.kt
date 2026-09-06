package fr.ftnl.cardgame.twitch

import fr.ftnl.cardgame.domain.card.CardId
import fr.ftnl.cardgame.domain.card.CardOrigin
import fr.ftnl.cardgame.domain.card.PunchlineCard
import fr.ftnl.cardgame.domain.card.SituationCard
import fr.ftnl.cardgame.domain.card.SituationText
import fr.ftnl.cardgame.domain.engine.GameCommand
import fr.ftnl.cardgame.domain.engine.GameEvent
import fr.ftnl.cardgame.domain.game.ChatCardSettings
import fr.ftnl.cardgame.domain.game.GameCode
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.game.GameListener
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Lets the viewers write cards instead of only judging them.
 *
 * `!situation le pire, c'est ____` and `!réponse un chat mouillé` drop straight into the
 * piles the table draws from, so a viewer's idea comes back a few minutes later in
 * somebody's hand. Whether that costs channel points, bits, or nothing at all is the
 * host's call, and it is read off the very same IRC line — see [ChatCardCommand].
 *
 * This keeps its own connection rather than riding on [TwitchChatVoting]: the vote only
 * listens while a round is being judged, and a chat writing cards is most useful in the
 * lobby, before there is a round at all.
 *
 * Nothing is kept beyond the game. Proposals are batched for a couple of seconds so a
 * lively chat becomes one command instead of forty, and a viewer only lands one card per
 * batch — a paid one gets through on the next.
 */
class TwitchChatCards(
    private val reader: TwitchChatReader,
    private val scope: CoroutineScope,
    private val flushMillis: Long = FLUSH_MILLIS,
    private val dispatch: suspend (GameCode, GameCommand) -> Unit,
) : GameListener {

    private val log = LoggerFactory.getLogger(javaClass)
    private val watched = ConcurrentHashMap<String, Watch>()

    override suspend fun onGameChanged(state: GameState, events: List<GameEvent>) {
        val channels = state.cardChannels
        if (channels.isEmpty()) close(state.code) else open(state, channels)
    }

    override suspend fun onGameForgotten(code: GameCode) = close(code)

    /**
     * One reading session per set of rules. A host who raises the price, closes the
     * situations or gains a guest chat gets a fresh session on the new terms rather than
     * a stale one still applying the old ones.
     */
    private fun open(state: GameState, channels: List<String>) {
        val terms = Terms(channels, state.settings.chatCards)
        synchronized(watched) {
            if (watched[state.code.value]?.terms == terms) return
            watched.remove(state.code.value)?.job?.cancel()
            val job = scope.launch { collect(state.code, terms) }
            watched[state.code.value] = Watch(terms, job)
        }
    }

    private fun close(code: GameCode) {
        synchronized(watched) { watched.remove(code.value)?.job?.cancel() }
    }

    private suspend fun collect(code: GameCode, terms: Terms) = coroutineScope {
        val pending = Basket()
        val flusher = launch {
            while (isActive) {
                delay(flushMillis)
                val batch = pending.take()
                if (batch.isEmpty()) continue
                dispatch(code, command(batch))
            }
        }
        try {
            keepReading(terms.channels) { line ->
                proposalOf(terms.rules, line)?.let(pending::offer)
            }
        } finally {
            flusher.cancel()
        }
    }

    private fun proposalOf(rules: ChatCardSettings, line: ChatLine): ChatProposal? {
        if (!ChatCardCommand.allows(rules, line)) return null
        val proposal = ChatCardCommand.parse(line) ?: return null
        return proposal.takeIf { ChatCardCommand.accepts(rules, it.kind) }
    }

    /** A card written by a chat is a card of this game and of no other, hence a throwaway id. */
    private fun command(batch: List<ChatProposal>) = GameCommand.AddChatCards(
        situations = batch.filter { it.kind == ChatCardKind.SITUATION }.map {
            SituationCard(CardId("chat-s-${UUID.randomUUID()}"), SituationText(it.text), CardOrigin.CHAT)
        },
        punchlines = batch.filter { it.kind == ChatCardKind.PUNCHLINE }.map {
            PunchlineCard(CardId("chat-p-${UUID.randomUUID()}"), it.text, CardOrigin.CHAT)
        },
    )

    /** A chat connection that drops is reopened: the table is still playing. */
    private suspend fun keepReading(channels: List<String>, onLine: suspend (ChatLine) -> Unit) {
        while (currentCoroutineContext().isActive) {
            try {
                reader.read(channels, onLine)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                log.warn("Twitch chat reading failed for {}, retrying", channels, failure)
            }
            delay(RETRY_MILLIS)
        }
    }

    private class Watch(val terms: Terms, val job: Job)

    private data class Terms(val channels: List<String>, val rules: ChatCardSettings)

    /**
     * What has come in since the last flush. One card per viewer per batch, so a single
     * enthusiastic chatter cannot fill a pile on their own — and, in the free mode, so a
     * bot pasting the same command in a loop lands one card and no more.
     */
    private class Basket {
        private val lock = Any()
        private val byViewer = linkedMapOf<String, ChatProposal>()

        fun offer(proposal: ChatProposal) = synchronized(lock) {
            if (byViewer.size < MAX_PER_BATCH) byViewer.putIfAbsent(proposal.viewerId, proposal)
            Unit
        }

        fun take(): List<ChatProposal> = synchronized(lock) {
            val batch = byViewer.values.toList()
            byViewer.clear()
            batch
        }
    }

    private companion object {
        const val FLUSH_MILLIS = 3_000L
        const val RETRY_MILLIS = 2_000L

        /** A ceiling per batch as well as the per game one the domain enforces. */
        const val MAX_PER_BATCH = 20
    }
}
