package fr.ftnl.cardgame.domain.engine

import fr.ftnl.cardgame.domain.deck.DrawPile
import fr.ftnl.cardgame.domain.game.GamePhase
import fr.ftnl.cardgame.domain.game.Round
import fr.ftnl.cardgame.domain.game.Scoreboard
import fr.ftnl.cardgame.domain.support.GameFixtures
import fr.ftnl.cardgame.domain.support.eventsOf
import fr.ftnl.cardgame.domain.support.perform
import fr.ftnl.cardgame.domain.support.refusal
import fr.ftnl.cardgame.domain.support.testEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameEngineReturnToLobbyTest {

    private val engine = testEngine()
    private val alice = GameFixtures.player("alice")
    private val bob = GameFixtures.player("bob")

    private fun finishedGame() = GameFixtures.lobby(listOf(alice, bob)).copy(
        phase = GamePhase.FINISHED,
        scoreboard = Scoreboard(mapOf(alice.id to 9, bob.id to 4)),
        hands = mapOf(alice.id to listOf(GameFixtures.punchline("h1"))),
        round = Round(number = 6, situation = GameFixtures.situation("s6")),
        situations = DrawPile(listOf(GameFixtures.situation("left1"))),
        punchlines = DrawPile(available = emptyList(), discarded = (1..40).map { GameFixtures.punchline("p$it") }),
        phaseDeadlineMillis = 1_234,
    )

    @Test
    fun `the host reopens an empty lobby with the scores wiped`() {
        val state = engine.perform(finishedGame(), GameCommand.ReturnToLobby(alice.id))

        assertEquals(GamePhase.LOBBY, state.phase)
        assertEquals(0, state.scoreboard.pointsOf(alice.id))
        assertEquals(0, state.scoreboard.pointsOf(bob.id))
        assertTrue(state.hands.isEmpty())
        assertNull(state.round)
        assertNull(state.phaseDeadlineMillis)
    }

    @Test
    fun `every card is gathered back so the next match can be dealt`() {
        val state = engine.perform(finishedGame(), GameCommand.ReturnToLobby(alice.id))

        assertEquals(41, state.punchlines.size)
        assertEquals(1, state.situations.size)
        assertTrue(engine.eventsOf(finishedGame(), GameCommand.ReturnToLobby(alice.id))
            .contains(GameEvent.ReturnedToLobby))
    }

    @Test
    fun `only the host may reopen the lobby`() {
        assertEquals(
            GameError.NOT_THE_HOST,
            engine.refusal(finishedGame(), GameCommand.ReturnToLobby(bob.id)),
        )
    }

    @Test
    fun `a game still running cannot be sent back to the lobby`() {
        val running = finishedGame().copy(phase = GamePhase.SELECTING)

        assertEquals(
            GameError.WRONG_PHASE,
            engine.refusal(running, GameCommand.ReturnToLobby(alice.id)),
        )
    }
}
