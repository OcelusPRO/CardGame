package fr.ftnl.cardgame.domain.engine

import fr.ftnl.cardgame.domain.card.CardId
import fr.ftnl.cardgame.domain.game.GamePhase
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.game.SubmissionId
import fr.ftnl.cardgame.domain.player.PlayerId
import fr.ftnl.cardgame.domain.support.eventsOf
import fr.ftnl.cardgame.domain.support.FixedClock
import fr.ftnl.cardgame.domain.support.GameFixtures
import fr.ftnl.cardgame.domain.support.perform
import fr.ftnl.cardgame.domain.support.refusal
import fr.ftnl.cardgame.domain.support.testEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GameEngineVoteRoundTest {

    private val clock = FixedClock(1_000_000)
    private val engine = testEngine(clock)
    private val players = GameFixtures.players("alice", "bob", "carl")
    private val alice = players[0].id
    private val bob = players[1].id
    private val carl = players[2].id

    private val running = startedGame()

    @Test
    fun `the answering step gets a deadline from the submit timer`() {
        val expected = 1_000_000 + running.settings.submitSeconds * 1000L

        assertEquals(expected, running.phaseDeadlineMillis)
    }

    @Test
    fun `playing a card takes it out of the hand`() {
        val state = engine.perform(running, GameCommand.PlayCards(alice, listOf(CardId("p1"))))

        assertEquals(running.settings.handSize - 1, state.handOf(alice).size)
        assertTrue(state.round?.hasSubmitted(alice) == true)
    }

    @Test
    fun `a player cannot answer twice`() {
        val state = engine.perform(running, GameCommand.PlayCards(alice, listOf(CardId("p1"))))

        val refusal = engine.refusal(state, GameCommand.PlayCards(alice, listOf(CardId("p2"))))

        assertEquals(GameError.ALREADY_SUBMITTED, refusal)
    }

    @Test
    fun `a player cannot play a card they do not hold`() {
        val refusal = engine.refusal(running, GameCommand.PlayCards(alice, listOf(CardId("p42"))))

        assertEquals(GameError.CARD_NOT_IN_HAND, refusal)
    }

    @Test
    fun `the number of answers must match the holes of the situation`() {
        val refusal = engine.refusal(running, GameCommand.PlayCards(alice, listOf(CardId("p1"), CardId("p2"))))

        assertEquals(GameError.WRONG_ANSWER_COUNT, refusal)
    }

    @Test
    fun `the last answer closes the step and shuffles the reveal order`() {
        val answered = everybodyAnswers()

        assertEquals(GamePhase.SELECTING, answered.phase)
        assertEquals(3, answered.round?.revealed?.size)
        assertEquals(1_000_000 + answered.settings.selectSeconds * 1000L, answered.phaseDeadlineMillis)
    }

    @Test
    fun `a player cannot vote for their own answer`() {
        val answered = everybodyAnswers()
        val own = assertNotNull(answered.round?.handleOf(alice))

        assertEquals(GameError.CANNOT_VOTE_OWN_ANSWER, engine.refusal(answered, GameCommand.Choose(alice, own)))
    }

    @Test
    fun `a vote on an unknown answer is refused`() {
        val answered = everybodyAnswers()

        assertEquals(
            GameError.UNKNOWN_SUBMISSION,
            engine.refusal(answered, GameCommand.Choose(alice, SubmissionId(9))),
        )
    }

    @Test
    fun `each vote scores a point and a unanimous answer adds the bonus`() {
        val scored = everybodyVotesFor(alice)

        assertEquals(GamePhase.ROUND_RESULT, scored.phase)
        assertEquals(2 + scored.settings.scoring.unanimityBonus, scored.scoreboard.pointsOf(alice))
        assertEquals(1, scored.scoreboard.pointsOf(bob))
        assertEquals(0, scored.scoreboard.pointsOf(carl))
    }

    @Test
    fun `closing a round reports what was played so statistics can be recorded`() {
        val answered = everybodyAnswers()
        val handles = handlesOf(answered)
        val firstVote = engine.perform(answered, GameCommand.Choose(bob, handles.getValue(alice)))
        val secondVote = engine.perform(firstVote, GameCommand.Choose(alice, handles.getValue(bob)))

        val events = engine.eventsOf(secondVote, GameCommand.Choose(carl, handles.getValue(alice)))

        val ended = events.filterIsInstance<GameEvent.RoundEnded>().single()
        assertEquals(3, ended.round.submissions.size)
        assertEquals(listOf(alice), ended.round.outcome?.winners)
    }

    @Test
    fun `the next round deals a new situation and refills the hands`() {
        val scored = everybodyVotesFor(alice)

        val next = engine.perform(scored, GameCommand.NextRound(alice))

        assertEquals(GamePhase.SUBMITTING, next.phase)
        assertEquals(2, next.round?.number)
        assertEquals(next.settings.handSize, next.handOf(alice).size)
    }

    @Test
    fun `the next round only reports the cards drawn to top the hands back up`() {
        val scored = everybodyVotesFor(alice)

        val events = engine.eventsOf(scored, GameCommand.NextRound(alice))

        val refilled = events.filterIsInstance<GameEvent.HandsRefilled>().single()
        assertEquals(3, refilled.punchlineCardIds.size)
    }

    @Test
    fun `only the host may ask for the next round`() {
        val scored = everybodyVotesFor(alice)

        assertEquals(GameError.NOT_THE_HOST, engine.refusal(scored, GameCommand.NextRound(bob)))
    }

    @Test
    fun `the scheduler may ask for the next round on its own`() {
        val scored = everybodyVotesFor(alice)

        assertEquals(GamePhase.SUBMITTING, engine.perform(scored, GameCommand.NextRound(null)).phase)
    }

    @Test
    fun `the game ends once the planned rounds have been played`() {
        val scored = everybodyVotesFor(alice).let { it.copy(settings = it.settings.copy(rounds = 1)) }

        val ended = engine.perform(scored, GameCommand.NextRound(alice))

        assertEquals(GamePhase.FINISHED, ended.phase)
    }

    @Test
    fun `a player cannot vote for their own answer by default`() {
        val answered = everybodyAnswers()
        val own = assertNotNull(answered.round?.handleOf(alice))

        assertEquals(GameError.CANNOT_VOTE_OWN_ANSWER, engine.refusal(answered, GameCommand.Choose(alice, own)))
    }

    @Test
    fun `self voting is accepted once the host allowed it`() {
        val permissive = running.copy(settings = running.settings.copy(allowSelfVote = true))
        val answered = listOf(alice to "p1", bob to "p11", carl to "p21")
            .fold(permissive) { state, (player, card) ->
                engine.perform(state, GameCommand.PlayCards(player, listOf(CardId(card))))
            }
        val own = assertNotNull(answered.round?.handleOf(alice))

        val voted = engine.perform(answered, GameCommand.Choose(alice, own))

        assertEquals(own, voted.round?.votes?.get(alice))
    }

    @Test
    fun `an expired submit timer closes the step with the answers received so far`() {
        val partial = engine.perform(running, GameCommand.PlayCards(alice, listOf(CardId("p1"))))

        val closed = engine.perform(partial, GameCommand.CloseSubmissions)

        assertEquals(GamePhase.SELECTING, closed.phase)
        assertEquals(1, closed.round?.revealed?.size)
    }

    @Test
    fun `an expired select timer scores the votes received so far`() {
        val answered = everybodyAnswers()
        val oneVote = engine.perform(answered, GameCommand.Choose(bob, handlesOf(answered).getValue(alice)))

        val closed = engine.perform(oneVote, GameCommand.CloseSelection)

        assertEquals(GamePhase.ROUND_RESULT, closed.phase)
        assertEquals(1 + closed.settings.scoring.unanimityBonus, closed.scoreboard.pointsOf(alice))
    }

    private fun startedGame(): GameState {
        val lobby = GameFixtures.lobby(players)
        val withCards = engine.perform(lobby, GameCommand.SetCardPool(alice, GameFixtures.pool()))
        return engine.perform(withCards, GameCommand.Start(alice))
    }

    private fun everybodyAnswers(): GameState = listOf(alice to "p1", bob to "p11", carl to "p21")
        .fold(running) { state, (player, card) ->
            engine.perform(state, GameCommand.PlayCards(player, listOf(CardId(card))))
        }

    private fun everybodyVotesFor(favourite: PlayerId): GameState {
        val answered = everybodyAnswers()
        val handles = handlesOf(answered)
        val other = handles.keys.first { it != favourite }
        return listOf(bob to favourite, carl to favourite, alice to other)
            .fold(answered) { state, (voter, choice) ->
                engine.perform(state, GameCommand.Choose(voter, handles.getValue(choice)))
            }
    }

    private fun handlesOf(state: GameState) =
        listOf(alice, bob, carl).associateWith { assertNotNull(state.round?.handleOf(it)) }
}
