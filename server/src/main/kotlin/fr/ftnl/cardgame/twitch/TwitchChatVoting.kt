package fr.ftnl.cardgame.twitch

import fr.ftnl.cardgame.domain.engine.GameCommand
import fr.ftnl.cardgame.domain.engine.GameEvent
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
 * Lets the Twitch chats vote alongside the table. While a round is being judged it reads
 * the watched channels, counts one voice per viewer, and pushes the running tally into
 * the game so every screen shows the same numbers.
 *
 * Nothing is kept: the counts live for the round, and a viewer is only ever remembered
 * long enough to stop them voting twice.
 */
class TwitchChatVoting(
    private val reader: TwitchChatReader,
    private val scope: CoroutineScope,
    private val flushMillis: Long = FLUSH_MILLIS,
    private val dispatch: suspend (GameCode, GameCommand) -> Unit,
) : GameListener {

    private val log = LoggerFactory.getLogger(javaClass)
    private val watched = ConcurrentHashMap<String, Watch>()

    override suspend fun onGameChanged(state: GameState, events: List<GameEvent>) {
        if (state.chatVoteOpen) open(state) else close(state.code)
    }

    override suspend fun onGameForgotten(code: GameCode) = close(code)

    /** One reading session per round: a new round means new numbers and a clean slate. */
    private fun open(state: GameState) {
        val round = state.round ?: return
        val answers = round.revealed.size.takeIf { it > 0 } ?: return
        synchronized(watched) {
            if (watched[state.code.value]?.round == round.number) return
            watched.remove(state.code.value)?.job?.cancel()
            val job = scope.launch { count(state.code, state.chatChannels, answers) }
            watched[state.code.value] = Watch(round.number, job)
        }
    }

    private fun close(code: GameCode) {
        synchronized(watched) { watched.remove(code.value)?.job?.cancel() }
    }

    private suspend fun count(code: GameCode, channels: List<String>, answers: Int) = coroutineScope {
        val tally = Tally()
        val flusher = launch {
            while (isActive) {
                delay(flushMillis)
                if (tally.takeChanged()) dispatch(code, GameCommand.SetChatVotes(tally.snapshot()))
            }
        }
        try {
            keepReading(channels) { line ->
                ChatVote.parse(line.text, answers)?.let { tally.record(line.channel, line.viewer, it) }
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

    private class Watch(val round: Int, val job: Job)

    /**
     * The counts being built, per channel. One viewer, one voice: their first vote is the
     * one that counts, so a chat cannot be flooded by a single very fast typist.
     */
    private class Tally {
        private val votes = ConcurrentHashMap<String, ConcurrentHashMap<SubmissionId, Int>>()
        private val voters = ConcurrentHashMap<String, MutableSet<String>>()
        private val changed = AtomicBoolean(false)

        fun record(channel: String, viewer: String, choice: SubmissionId) {
            val seen = voters.computeIfAbsent(channel) { ConcurrentHashMap.newKeySet() }
            if (!seen.add(viewer)) return
            votes.computeIfAbsent(channel) { ConcurrentHashMap() }.merge(choice, 1, Int::plus)
            changed.set(true)
        }

        fun takeChanged(): Boolean = changed.getAndSet(false)

        fun snapshot(): Map<String, Map<SubmissionId, Int>> = votes.mapValues { it.value.toMap() }
    }

    private companion object {
        const val FLUSH_MILLIS = 1_000L
        const val RETRY_MILLIS = 2_000L
    }
}
