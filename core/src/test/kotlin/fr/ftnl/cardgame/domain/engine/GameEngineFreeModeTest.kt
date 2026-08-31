package fr.ftnl.cardgame.domain.engine

import fr.ftnl.cardgame.domain.card.CardId
import fr.ftnl.cardgame.domain.card.CardPool
import fr.ftnl.cardgame.domain.game.AnswerMode
import fr.ftnl.cardgame.domain.game.GamePhase
import fr.ftnl.cardgame.domain.game.GameSettings
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.support.GameFixtures
import fr.ftnl.cardgame.domain.support.perform
import fr.ftnl.cardgame.domain.support.refusal
import fr.ftnl.cardgame.domain.support.testEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** "Sans limites": no punchline deck at all, players type their own answer. */
class GameEngineFreeModeTest {

    private val engine = testEngine()
    private val players = GameFixtures.players("alice", "bob", "carl")
    private val alice = players[0].id
    private val bob = players[1].id
    private val carl = players[2].id

    private val running = startedGame()

    @Test
    fun `nobody gets a hand of cards`() {
        assertTrue(running.hands.isEmpty())
    }

    @Test
    fun `a game without punchline cards still starts`() {
        assertEquals(GamePhase.SUBMITTING, running.phase)
    }

    @Test
    fun `a written answer is registered`() {
        val state = engine.perform(running, GameCommand.WriteAnswers(alice, listOf("un chat en costume")))

        assertEquals(listOf("un chat en costume"), state.round?.submissions?.get(alice)?.answers)
    }

    @Test
    fun `a blank answer is refused`() {
        assertEquals(
            GameError.INVALID_ANSWER,
            engine.refusal(running, GameCommand.WriteAnswers(alice, listOf("   "))),
        )
    }

    @Test
    fun `an endless answer is refused`() {
        val tooLong = "a".repeat(200)

        assertEquals(
            GameError.INVALID_ANSWER,
            engine.refusal(running, GameCommand.WriteAnswers(alice, listOf(tooLong))),
        )
    }

    @Test
    fun `playing a card is refused in free mode`() {
        assertEquals(
            GameError.WRONG_PHASE,
            engine.refusal(running, GameCommand.PlayCards(alice, listOf(CardId("p1")))),
        )
    }

    @Test
    fun `the round runs to its end just like with cards`() {
        val answered = listOf(alice to "réponse A", bob to "réponse B", carl to "réponse C")
            .fold(running) { state, (player, text) ->
                engine.perform(state, GameCommand.WriteAnswers(player, listOf(text)))
            }

        assertEquals(GamePhase.SELECTING, answered.phase)
        assertEquals(3, answered.round?.revealed?.size)
    }

    private fun startedGame(): GameState {
        val settings = GameSettings(minPlayers = 2, answerMode = AnswerMode.FREE_TEXT)
        val lobby = GameFixtures.lobby(players, settings)
        val pool = CardPool(GameFixtures.pool().situations, emptyList())
        val withCards = engine.perform(lobby, GameCommand.SetCardPool(alice, pool))
        return engine.perform(withCards, GameCommand.Start(alice))
    }
}
