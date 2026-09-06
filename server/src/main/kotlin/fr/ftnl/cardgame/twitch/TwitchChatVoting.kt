package fr.ftnl.cardgame.twitch

import fr.ftnl.cardgame.domain.engine.GameEvent
import fr.ftnl.cardgame.domain.game.ChatVoteScope
import fr.ftnl.cardgame.domain.game.ChatVoteTally
import fr.ftnl.cardgame.domain.game.ChatVoter
import fr.ftnl.cardgame.domain.game.GameCode
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.game.SubmissionId
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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Lets the Twitch chats judge. While a vote is open it reads the watched channels, counts
 * one voice per viewer, and publishes the running tally so every screen shows the same
 * numbers and the same faces.
 *
 * The viewers type a **position** on screen, so a chat judging a ladder picks between `1`
 * and `2` rather than hunting for a card among twelve — which is the pairing of a big chat
 * with the duel format, and the reason the two are separate settings.
 *
 * The tally goes to a [ChatVoteBoard] and out on its own frame, never through the game
 * snapshot: a number that moves once a second has no business re-encoding a deck of cards
 * and travelling to Redis. It is banked into the round once, when it becomes a score.
 *
 * Nothing is kept: the counts live for the round, and a viewer is only ever remembered
 * long enough to stop them voting twice.
 */
class TwitchChatVoting(
    private val reader: TwitchChatReader,
    private val scope: CoroutineScope,
    private val pictures: ViewerPictures = ViewerPictures.NONE,
    private val flushMillis: Long = FLUSH_MILLIS,
    private val board: ChatVoteBoard,
    private val live: ChatVoteLive = ChatVoteLive.NONE,
) : GameListener {

    private val log = LoggerFactory.getLogger(javaClass)
    private val watched = ConcurrentHashMap<String, Watch>()

    override suspend fun onGameChanged(state: GameState, events: List<GameEvent>) {
        if (state.chatVoteOpen) open(state) else close(state.code)
    }

    /**
     * One reading session per stretch of the game the chat is judging: a round when the
     * answers are shown all at once, a **duel** when they are shown two at a time. Either
     * way, a new stretch means new numbers and a clean slate — a viewer who typed `2` on
     * the last duel has not voted on this one.
     */
    private fun open(state: GameState) {
        val scopeOf = state.chatVoteScope ?: return
        val choices = state.chatChoices.takeIf { it.isNotEmpty() } ?: return
        synchronized(watched) {
            if (watched[state.code.value]?.scope == scopeOf) return
            watched.remove(state.code.value)?.job?.cancel()
            val job = scope.launch { count(state.code, scopeOf, state.chatChannels, choices) }
            watched[state.code.value] = Watch(scopeOf, job)
        }
    }

    private fun close(code: GameCode) {
        synchronized(watched) { watched.remove(code.value)?.job?.cancel() }
    }

    override suspend fun onGameForgotten(code: GameCode) = close(code)

    private suspend fun count(
        code: GameCode,
        scopeOf: ChatVoteScope,
        channels: List<String>,
        choices: List<SubmissionId>,
    ) = coroutineScope {
        val tally = Tally()
        val flusher = launch {
            while (isActive) {
                delay(flushMillis)
                if (!tally.takeChanged()) continue
                // Only the faces the table will show are ever looked up.
                tally.withPictures(pictures.of(tally.facesWithoutPicture()))
                // The board is memory and the push is one small frame: a running count
                // never re-encodes the deck, and never reaches Redis. It is banked into
                // the snapshot once, by the scheduler, at the moment it becomes a score.
                val snapshot = tally.snapshot()
                board.publish(code, scopeOf, snapshot)
                live.push(code, snapshot)
            }
        }
        try {
            keepReading(channels) { line ->
                ChatVote.parse(line.text, choices)?.let { choice ->
                    tally.record(ChatVoter(line.viewerId, line.viewerName), choice)
                }
            }
        } finally {
            flusher.cancel()
        }
    }

    /** A chat connection that drops mid vote is reopened: the round is still running. */
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

    private class Watch(val scope: ChatVoteScope, val job: Job)

    /**
     * The counts being built. One viewer, one voice, wherever they typed it: their first
     * vote is the one that counts, so nobody votes twice by switching chat or by typing
     * faster than everybody else.
     *
     * Only the first [ChatVoteTally.MAX_FACES] voters of an answer are kept by name: they
     * are the faces the table shows, and the rest of a large chat is a number.
     */
    private class Tally {
        private val lock = Any()
        private val heard = mutableSetOf<String>()
        private val counts = mutableMapOf<SubmissionId, Int>()
        private val faces = mutableMapOf<SubmissionId, MutableList<ChatVoter>>()
        private val changed = AtomicBoolean(false)

        fun record(voter: ChatVoter, choice: SubmissionId) = synchronized(lock) {
            if (!heard.add(voter.id)) return
            counts[choice] = (counts[choice] ?: 0) + 1
            val shown = faces.getOrPut(choice) { mutableListOf() }
            if (shown.size < ChatVoteTally.MAX_FACES) shown += voter
            changed.set(true)
        }

        fun facesWithoutPicture(): List<String> = synchronized(lock) {
            faces.values.flatten().filter { it.avatarUrl == null }.map { it.id }
        }

        fun withPictures(found: Map<String, String>) = synchronized(lock) {
            if (found.isEmpty()) return
            faces.values.forEach { shown ->
                shown.forEachIndexed { index, voter ->
                    found[voter.id]?.let { shown[index] = voter.copy(avatarUrl = it) }
                }
            }
        }

        fun takeChanged(): Boolean = changed.getAndSet(false)

        fun snapshot(): Map<SubmissionId, ChatVoteTally> = synchronized(lock) {
            counts.mapValues { (choice, count) ->
                ChatVoteTally(count = count, voters = faces[choice].orEmpty().toList())
            }
        }
    }

    private companion object {
        const val FLUSH_MILLIS = 1_000L
        const val RETRY_MILLIS = 2_000L
    }
}
