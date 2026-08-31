package fr.ftnl.cardgame.domain.engine

import fr.ftnl.cardgame.domain.card.CardId
import fr.ftnl.cardgame.domain.game.GamePhase
import fr.ftnl.cardgame.domain.game.GameSettings
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.game.SelectionMode
import fr.ftnl.cardgame.domain.support.GameFixtures
import fr.ftnl.cardgame.domain.support.perform
import fr.ftnl.cardgame.domain.support.refusal
import fr.ftnl.cardgame.domain.support.testEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** The CZAR mode variant where [GameSettings.czarAnswers] lets the judge also play a card. */
class GameEngineCzarAnswersRoundTest {

    private val engine = testEngine()
    private val players = GameFixtures.players("alice", "bob", "carl")
    private val alice = players[0].id
    private val bob = players[1].id
    private val carl = players[2].id

    private val running = startedGame()

    @Test
    fun `the czar may now play a card`() {
        val state = engine.perform(running, GameCommand.PlayCards(alice, listOf(CardId("p1"))))

        assertEquals(true, state.round?.hasSubmitted(alice))
    }

    @Test
    fun `the step waits for the czar too`() {
        val others = listOf(bob to "p11", carl to "p21").fold(running) { state, (player, card) ->
            engine.perform(state, GameCommand.PlayCards(player, listOf(CardId(card))))
        }
        assertEquals(GamePhase.SUBMITTING, others.phase)

        val all = engine.perform(others, GameCommand.PlayCards(alice, listOf(CardId("p1"))))
        assertEquals(GamePhase.SELECTING, all.phase)
    }

    @Test
    fun `the czar cannot pick their own answer`() {
        val answered = everybodyAnswers()
        val ownHandle = assertNotNull(answered.round?.handleOf(alice))

        assertEquals(
            GameError.CANNOT_VOTE_OWN_ANSWER,
            engine.refusal(answered, GameCommand.Choose(alice, ownHandle)),
        )
    }

    @Test
    fun `the czar still awards the points to the answer they pick`() {
        val answered = everybodyAnswers()
        val handle = assertNotNull(answered.round?.handleOf(bob))

        val scored = engine.perform(answered, GameCommand.Choose(alice, handle))

        assertEquals(GamePhase.ROUND_RESULT, scored.phase)
        assertEquals(scored.settings.scoring.pointsPerVote, scored.scoreboard.pointsOf(bob))
        assertEquals(0, scored.scoreboard.pointsOf(alice))
    }

    private fun startedGame(): GameState {
        val settings = GameSettings(
            minPlayers = 2,
            selectionMode = SelectionMode.CZAR,
            czarAnswers = true,
        )
        val lobby = GameFixtures.lobby(players, settings)
        val withCards = engine.perform(lobby, GameCommand.SetCardPool(alice, GameFixtures.pool()))
        return engine.perform(withCards, GameCommand.Start(alice))
    }

    private fun everybodyAnswers(): GameState =
        listOf(alice to "p1", bob to "p11", carl to "p21").fold(running) { state, (player, card) ->
            engine.perform(state, GameCommand.PlayCards(player, listOf(CardId(card))))
        }
}
