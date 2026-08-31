package fr.ftnl.cardgame.domain.engine

import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.game.SelectionMode
import fr.ftnl.cardgame.domain.player.PlayerId

/**
 * Answers "is the current step over?" and "who are we still waiting for?".
 * Shared by the engine, which auto advances, and by the server, which shows the wait list.
 */
object RoundProgress {

    /** Every connected player expected to answer has answered. */
    fun submissionsComplete(state: GameState): Boolean {
        val round = state.round ?: return false
        val expected = state.answeringPlayers
        return expected.isNotEmpty() && expected.all { round.hasSubmitted(it.id) }
    }

    /** Every eligible voter has chosen, or there was simply nothing to choose from. */
    fun selectionComplete(state: GameState): Boolean {
        val round = state.round ?: return false
        if (round.revealOrder.isEmpty()) return true
        return voters(state).all(round::hasVoted)
    }

    /**
     * Who may vote: the czar alone, or every connected player. Without self voting, a
     * player is only a voter once there is an answer on the table that is not their own.
     */
    fun voters(state: GameState): List<PlayerId> {
        val round = state.round ?: return emptyList()
        return when (state.settings.selectionMode) {
            SelectionMode.CZAR -> listOfNotNull(round.czarId)
            SelectionMode.VOTE -> state.connectedPlayers.map { it.id }.filter { voter ->
                state.settings.allowSelfVote || round.revealed.any { (_, answer) -> answer.playerId != voter }
            }
        }
    }

    fun pendingAnswers(state: GameState): List<PlayerId> {
        val round = state.round ?: return emptyList()
        return state.answeringPlayers.map { it.id }.filterNot(round::hasSubmitted)
    }

    fun pendingVotes(state: GameState): List<PlayerId> {
        val round = state.round ?: return emptyList()
        return voters(state).filterNot(round::hasVoted)
    }
}
