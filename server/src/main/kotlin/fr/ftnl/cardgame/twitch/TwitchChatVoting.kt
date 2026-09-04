package fr.ftnl.cardgame.twitch

import fr.ftnl.cardgame.domain.engine.GameCommand
import fr.ftnl.cardgame.domain.engine.GameEvent
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
 * Lets the Twitch chats judge the round. While a round is being voted on it reads the
 * watched channels, counts one voice per viewer, and pushes the running tally into the
 * game so every screen shows the same numbers and the same faces.
 *
 * Nothing is kept: the counts live for the round, and a viewer is only ever remembered
 * long enough to stop them voting twice.
 */
class TwitchChatVoting(
    private val reader: TwitchChatReader,
    private val scope: CoroutineScope,
    private val pictures: ViewerPictures = ViewerPictures.NONE,
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
                if (!tally.takeChanged()) continue
                // Only the faces the table will show are ever looked up.
                tally.withPictures(pictures.of(tally.facesWithoutPicture()))
                dispatch(code, GameCommand.SetChatVotes(tally.snapshot()))
            }
        }
        try {
            keepReading(channels) { line ->
                ChatVote.parse(line.text, answers)?.let { choice ->
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

    private class Watch(val round: Int, val job: Job)

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
