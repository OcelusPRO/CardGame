package fr.ftnl.cardgame.twitch

import fr.ftnl.cardgame.domain.engine.GameCommand
import fr.ftnl.cardgame.domain.game.ChatVoteScope
import fr.ftnl.cardgame.domain.game.ChatVoteTally
import fr.ftnl.cardgame.domain.game.GameCode
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.game.SubmissionId
import fr.ftnl.cardgame.game.GameListener
import java.util.concurrent.ConcurrentHashMap

/**
 * Where the running chat tally lives while a round is being voted on.
 *
 * It is deliberately **not** the game snapshot. A tally moves several times a second, and
 * pushing it through the snapshot meant re-encoding the whole state — deck included, up to
 * a thousand cards on a table playing its own — and writing it back to Redis at that same
 * cadence, for a number that is only ever shown on screen.
 *
 * So the count lives here, in memory, and is broadcast on its own small frame. It joins
 * the snapshot exactly once, when the judging closes and the tally stops being a display
 * and starts being a score: [commitFor] is what the scheduler runs just before closing.
 *
 * On a ladder that happens once per **duel**, not once per round, which is why a tally is
 * stamped with the stretch it covers rather than with a round number.
 */
class ChatVoteBoard : GameListener {

    private val tallies = ConcurrentHashMap<String, Snapshot>()

    /** Replaces the running tally of a game. The scope guards against a late frame. */
    fun publish(code: GameCode, scope: ChatVoteScope, values: Map<SubmissionId, ChatVoteTally>) {
        tallies[code.value] = Snapshot(scope, values)
    }

    fun of(code: GameCode): Map<SubmissionId, ChatVoteTally> = tallies[code.value]?.values.orEmpty()

    /**
     * The command that banks the tally into the round about to be scored, or null when
     * this game has no chat judging it — which is every game but a `CHAT` one.
     */
    fun commitFor(state: GameState): GameCommand? {
        val snapshot = tallies[state.code.value] ?: return null
        // A tally counted for the duel before this one has no business deciding this one.
        if (snapshot.scope != state.chatVoteScope) return null
        return GameCommand.SetChatVotes(snapshot.values)
    }

    fun forget(code: GameCode) {
        tallies.remove(code.value)
    }

    override suspend fun onGameForgotten(code: GameCode) = forget(code)

    /** How many games hold a live tally; a table that moved on should drop out of it. */
    val size: Int get() = tallies.size

    private class Snapshot(val scope: ChatVoteScope, val values: Map<SubmissionId, ChatVoteTally>)
}

/** Pushes a running tally to the screens watching a table, without touching the store. */
fun interface ChatVoteLive {
    suspend fun push(code: GameCode, tallies: Map<SubmissionId, ChatVoteTally>)

    companion object {
        val NONE = ChatVoteLive { _, _ -> }
    }
}
