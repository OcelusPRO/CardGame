package fr.ftnl.cardgame.domain.engine

import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.game.Round
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

    /**
     * Every eligible voter has chosen, or there was simply nothing to choose from.
     *
     * When the chat judges, the step never ends early: nobody at the table votes, and the
     * viewers need the whole timer to read the answers, so the timer alone ends it.
     */
    fun selectionComplete(state: GameState): Boolean {
        val round = state.round ?: return false
        if (round.revealOrder.isEmpty()) return true
        // When the chat judges, the step never ends early, whatever the format: the
        // viewers need the clock to read what is in front of them.
        if (state.settings.selectionMode == SelectionMode.CHAT) return false
        if (state.settings.runsBracket) {
            val duel = round.bracket?.current ?: return true
            // An empty table would otherwise walk the whole ladder in one go, the instant
            // the last player dropped: with nobody to vote, the timer runs the duels.
            val voters = voters(state)
            return voters.isNotEmpty() && voters.all(duel::hasVoted)
        }
        return voters(state).all(round::hasVoted)
    }

    /**
     * Who may vote: the czar alone, every connected player, or — when the chat judges —
     * nobody at all. Without self voting, a player is only a voter once there is an
     * answer on the table that is not their own.
     */
    fun voters(state: GameState): List<PlayerId> {
        val round = state.round ?: return emptyList()
        val candidates = when (state.settings.selectionMode) {
            SelectionMode.CHAT -> return emptyList()
            SelectionMode.CZAR -> listOfNotNull(round.czarId)
            SelectionMode.VOTE -> state.connectedPlayers.map { it.id }
        }
        return candidates.filter { voter -> hasSomethingToPick(state, round, voter) }
    }

    /**
     * A voter needs at least one answer in front of them that is theirs to pick — which
     * is only ever the two of the open duel when the round runs as a ladder.
     *
     * Nobody judges a duel their own card is in, neither for it nor against it: the author
     * cannot vote for themselves in any mode, and their opponent would be left with nothing
     * to choose but the card beating theirs, which is a formality rather than a vote.
     */
    private fun hasSomethingToPick(state: GameState, round: Round, voter: PlayerId): Boolean {
        if (state.settings.allowSelfVote) return true
        if (state.settings.runsBracket) {
            val duel = round.bracket?.current ?: return false
            return voter !in round.authorsOf(duel)
        }
        return round.revealed.any { (_, answer) -> answer.playerId != voter }
    }

    fun pendingAnswers(state: GameState): List<PlayerId> {
        val round = state.round ?: return emptyList()
        return state.answeringPlayers.map { it.id }.filterNot(round::hasSubmitted)
    }

    fun pendingVotes(state: GameState): List<PlayerId> =
        voters(state).filterNot(state::hasVoted)
}
