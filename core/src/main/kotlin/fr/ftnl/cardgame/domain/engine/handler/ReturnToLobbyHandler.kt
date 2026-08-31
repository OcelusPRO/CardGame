package fr.ftnl.cardgame.domain.engine.handler

import fr.ftnl.cardgame.domain.deck.DrawPile
import fr.ftnl.cardgame.domain.deck.Shuffler
import fr.ftnl.cardgame.domain.engine.CommandResult
import fr.ftnl.cardgame.domain.engine.GameCommand
import fr.ftnl.cardgame.domain.engine.GameError
import fr.ftnl.cardgame.domain.engine.GameEvent
import fr.ftnl.cardgame.domain.game.GamePhase
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.game.Scoreboard

/**
 * Reopens the lobby of a finished game. Scores are wiped and every card is gathered back
 * into a freshly shuffled pile, so the same table can play another match without rejoining.
 */
internal class ReturnToLobbyHandler(private val shuffler: Shuffler) {

    fun handle(state: GameState, command: GameCommand.ReturnToLobby): CommandResult {
        if (!state.isHost(command.by)) return CommandResult.rejected(GameError.NOT_THE_HOST)
        if (state.phase != GamePhase.FINISHED) return CommandResult.rejected(GameError.WRONG_PHASE)
        val punchlines = state.punchlines.available + state.punchlines.discarded +
            state.hands.values.flatten()
        return CommandResult.accepted(
            state.copy(
                phase = GamePhase.LOBBY,
                scoreboard = state.players.fold(Scoreboard()) { board, p -> board.withPlayer(p.id) },
                hands = emptyMap(),
                round = null,
                phaseDeadlineMillis = null,
                situations = DrawPile.shuffled(
                    state.situations.available + state.situations.discarded,
                    shuffler,
                ),
                punchlines = DrawPile.shuffled(punchlines, shuffler),
            ),
            GameEvent.ReturnedToLobby,
        )
    }
}
