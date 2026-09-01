package fr.ftnl.cardgame.domain.engine

import fr.ftnl.cardgame.domain.deck.RandomShuffler
import fr.ftnl.cardgame.domain.deck.Shuffler
import fr.ftnl.cardgame.domain.engine.handler.AnswerHandler
import fr.ftnl.cardgame.domain.engine.handler.CardPoolHandler
import fr.ftnl.cardgame.domain.engine.handler.ChoiceHandler
import fr.ftnl.cardgame.domain.engine.handler.ConnectionHandler
import fr.ftnl.cardgame.domain.engine.handler.DropIfAwayHandler
import fr.ftnl.cardgame.domain.engine.handler.JoinHandler
import fr.ftnl.cardgame.domain.engine.handler.KickHandler
import fr.ftnl.cardgame.domain.engine.handler.LeaveHandler
import fr.ftnl.cardgame.domain.engine.handler.ReturnToLobbyHandler
import fr.ftnl.cardgame.domain.engine.handler.RoundFlowHandler
import fr.ftnl.cardgame.domain.engine.handler.SettingsHandler
import fr.ftnl.cardgame.domain.engine.handler.StartHandler
import fr.ftnl.cardgame.domain.game.GameClock
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.game.SystemGameClock

/**
 * The only entry point of the game rules. It is pure: give it a snapshot and a command,
 * it hands back the next snapshot, never touching a database, a socket or a timer.
 */
class GameEngine(
    shuffler: Shuffler = RandomShuffler(),
    clock: GameClock = SystemGameClock,
) {
    private val roundStarter = RoundStarter(shuffler, clock)
    private val roundFlow = RoundFlow(SubmissionCloser(shuffler, clock), SelectionCloser(clock))

    private val join = JoinHandler()
    private val leave = LeaveHandler(roundFlow)
    private val connection = ConnectionHandler(roundFlow)
    private val dropIfAway = DropIfAwayHandler(roundFlow)
    private val kick = KickHandler()
    private val settings = SettingsHandler()
    private val cardPool = CardPoolHandler(shuffler)
    private val start = StartHandler(roundStarter)
    private val answer = AnswerHandler(roundFlow)
    private val choice = ChoiceHandler(roundFlow)
    private val flow = RoundFlowHandler(roundFlow, roundStarter)
    private val returnToLobby = ReturnToLobbyHandler(shuffler)

    fun execute(state: GameState, command: GameCommand): CommandResult = when (command) {
        is GameCommand.Join -> join.handle(state, command)
        is GameCommand.Leave -> leave.handle(state, command)
        is GameCommand.SetConnected -> connection.handle(state, command)
        is GameCommand.DropIfAway -> dropIfAway.handle(state, command)
        is GameCommand.Kick -> kick.handle(state, command)
        is GameCommand.UpdateSettings -> settings.handle(state, command)
        is GameCommand.SetCardPool -> cardPool.handle(state, command)
        is GameCommand.Start -> start.handle(state, command)
        is GameCommand.PlayCards -> answer.playCards(state, command)
        is GameCommand.WriteAnswers -> answer.writeAnswers(state, command)
        is GameCommand.Choose -> choice.handle(state, command)
        GameCommand.CloseSubmissions -> flow.closeSubmissions(state)
        GameCommand.CloseSelection -> flow.closeSelection(state)
        is GameCommand.NextRound -> flow.nextRound(state, command)
        is GameCommand.ReturnToLobby -> returnToLobby.handle(state, command)
    }
}
