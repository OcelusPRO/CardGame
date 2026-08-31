package fr.ftnl.cardgame.domain.support

import fr.ftnl.cardgame.domain.deck.IdentityShuffler
import fr.ftnl.cardgame.domain.engine.CommandResult
import fr.ftnl.cardgame.domain.engine.GameCommand
import fr.ftnl.cardgame.domain.engine.GameEngine
import fr.ftnl.cardgame.domain.engine.GameError
import fr.ftnl.cardgame.domain.engine.GameEvent
import fr.ftnl.cardgame.domain.game.GameState
import kotlin.test.assertIs

/** A fully predictable engine: no shuffling, no wall clock. */
fun testEngine(clock: FixedClock = FixedClock()): GameEngine = GameEngine(IdentityShuffler, clock)

/** Runs a command and fails the test if it was refused. */
fun GameEngine.perform(state: GameState, command: GameCommand): GameState =
    assertIs<CommandResult.Accepted>(execute(state, command), "command $command was refused").state

/** Runs a command and returns the events it produced. */
fun GameEngine.eventsOf(state: GameState, command: GameCommand): List<GameEvent> =
    assertIs<CommandResult.Accepted>(execute(state, command)).events

/** Runs a command that must be refused and returns the reason. */
fun GameEngine.refusal(state: GameState, command: GameCommand): GameError =
    assertIs<CommandResult.Rejected>(execute(state, command), "command $command was accepted").error
