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

class GameEngineCzarRoundTest {

    private val engine = testEngine()
    private val players = GameFixtures.players("alice", "bob", "carl")
    private val alice = players[0].id
    private val bob = players[1].id
    private val carl = players[2].id

    private val running = startedGame()

    @Test
    fun `the first czar is the first player at the table`() {
        assertEquals(alice, running.round?.czarId)
    }

    @Test
    fun `the czar does not answer`() {
        val refusal = engine.refusal(running, GameCommand.PlayCards(alice, listOf(CardId("p1"))))

        assertEquals(GameError.CZAR_CANNOT_ANSWER, refusal)
    }

    @Test
    fun `the step closes when every other player answered`() {
        assertEquals(GamePhase.SELECTING, everybodyAnswers().phase)
    }

    @Test
    fun `only the czar may pick the winner`() {
        val answered = everybodyAnswers()
        val handle = assertNotNull(answered.round?.handleOf(bob))

        assertEquals(GameError.NOT_THE_CZAR, engine.refusal(answered, GameCommand.Choose(carl, handle)))
    }

    @Test
    fun `the picked answer wins the czar points`() {
        val answered = everybodyAnswers()
        val handle = assertNotNull(answered.round?.handleOf(carl))

        val scored = engine.perform(answered, GameCommand.Choose(alice, handle))

        assertEquals(GamePhase.ROUND_RESULT, scored.phase)
        assertEquals(scored.settings.scoring.pointsPerVote, scored.scoreboard.pointsOf(carl))
        assertEquals(0, scored.scoreboard.pointsOf(bob))
    }

    @Test
    fun `the crown moves to the next player on the following round`() {
        val answered = everybodyAnswers()
        val handle = assertNotNull(answered.round?.handleOf(carl))
        val scored = engine.perform(answered, GameCommand.Choose(alice, handle))

        val next = engine.perform(scored, GameCommand.NextRound(alice))

        assertEquals(bob, next.round?.czarId)
    }

    private fun startedGame(): GameState {
        val settings = GameSettings(minPlayers = 2, selectionMode = SelectionMode.CZAR)
        val lobby = GameFixtures.lobby(players, settings)
        val withCards = engine.perform(lobby, GameCommand.SetCardPool(alice, GameFixtures.pool()))
        return engine.perform(withCards, GameCommand.Start(alice))
    }

    private fun everybodyAnswers(): GameState = listOf(bob to "p11", carl to "p21")
        .fold(running) { state, (player, card) ->
            engine.perform(state, GameCommand.PlayCards(player, listOf(CardId(card))))
        }
}
