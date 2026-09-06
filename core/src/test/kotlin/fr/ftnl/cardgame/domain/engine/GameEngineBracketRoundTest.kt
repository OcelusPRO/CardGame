package fr.ftnl.cardgame.domain.engine

import fr.ftnl.cardgame.domain.card.CardId
import fr.ftnl.cardgame.domain.game.GamePhase
import fr.ftnl.cardgame.domain.game.GameSettings
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.game.SelectionFormat
import fr.ftnl.cardgame.domain.game.SubmissionId
import fr.ftnl.cardgame.domain.support.FixedClock
import fr.ftnl.cardgame.domain.support.GameFixtures
import fr.ftnl.cardgame.domain.support.perform
import fr.ftnl.cardgame.domain.support.refusal
import fr.ftnl.cardgame.domain.support.testEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The duel format on top of the default "everybody votes", on the table it was written for:
 * four answers, three duels, and a point for every duel actually won.
 *
 * The shuffler is the identity one, so the reveal order is the seating order and the
 * pairing is `alice vs bob`, `carl vs dave` — which is what makes these assertions
 * readable rather than a lottery.
 */
class GameEngineBracketRoundTest {

    private val clock = FixedClock(1_000_000)
    private val engine = testEngine(clock)
    private val players = GameFixtures.players("alice", "bob", "carl", "dave")
    private val alice = players[0].id
    private val bob = players[1].id
    private val carl = players[2].id
    private val dave = players[3].id

    private val answered = everybodyAnswers()

    @Test
    fun `closing the answers opens a ladder and times the first duel alone`() {
        val bracket = assertNotNull(answered.round?.bracket)

        assertEquals(GamePhase.SELECTING, answered.phase)
        assertEquals(2, bracket.duels.size)
        assertEquals(SubmissionId(0), bracket.current?.left)
        assertEquals(SubmissionId(1), bracket.current?.right)
        // A duel, not the whole judging: the clock is the short one.
        assertEquals(1_000_000 + answered.settings.duelSeconds * 1000L, answered.phaseDeadlineMillis)
    }

    @Test
    fun `only the two answers on the table may be picked`() {
        val refusal = engine.refusal(answered, GameCommand.Choose(carl, SubmissionId(2)))

        assertEquals(GameError.NOT_IN_THIS_DUEL, refusal)
    }

    @Test
    fun `an author still cannot vote for their own answer`() {
        val own = assertNotNull(answered.round?.handleOf(alice))

        assertEquals(
            GameError.CANNOT_VOTE_OWN_ANSWER,
            engine.refusal(answered, GameCommand.Choose(alice, own)),
        )
    }

    @Test
    fun `the last vote of a duel settles it and puts the next pair up`() {
        val settled = duelOne()
        val bracket = assertNotNull(settled.round?.bracket)

        assertEquals(GamePhase.SELECTING, settled.phase, "the round ended on the first duel")
        assertEquals(SubmissionId(2), bracket.current?.left)
        assertEquals(SubmissionId(3), bracket.current?.right)
        assertEquals(1, bracket.winsOf(SubmissionId(0)))
    }

    @Test
    fun `an author sits out the duel their own card is in`() {
        val refusal = engine.refusal(answered, GameCommand.Choose(bob, SubmissionId(0)))

        assertEquals(GameError.CANNOT_JUDGE_OWN_DUEL, refusal)
    }

    @Test
    fun `a vote is scoped to its duel, so a player barred from one judges the next`() {
        val settled = duelOne()

        val voted = engine.perform(settled, GameCommand.Choose(alice, SubmissionId(2)))

        assertTrue(voted.hasVoted(alice), "alice should be counted in the second duel")
    }

    @Test
    fun `a player only votes once inside one duel`() {
        val voted = engine.perform(answered, GameCommand.Choose(carl, SubmissionId(0)))

        assertEquals(
            GameError.ALREADY_VOTED,
            engine.refusal(voted, GameCommand.Choose(carl, SubmissionId(1))),
        )
    }

    @Test
    fun `every duel won is a point, and the final crowns the round`() {
        val scored = wholeLadder()

        assertEquals(GamePhase.ROUND_RESULT, scored.phase)
        // alice wins her first duel then the final; carl wins his, then loses.
        assertEquals(2, scored.scoreboard.pointsOf(alice))
        assertEquals(1, scored.scoreboard.pointsOf(carl))
        assertEquals(0, scored.scoreboard.pointsOf(bob))
        assertEquals(0, scored.scoreboard.pointsOf(dave))
        assertEquals(listOf(alice), scored.round?.outcome?.winners)
    }

    @Test
    fun `a duel nobody voted in advances the first answer without paying for it`() {
        // Nobody votes; the timer is what closes a duel then, twice over, then the final.
        val ladder = generateSequence(answered) { state ->
            state.takeIf { it.phase == GamePhase.SELECTING }
                ?.let { engine.perform(it, GameCommand.CloseSelection) }
        }.last()

        assertEquals(GamePhase.ROUND_RESULT, ladder.phase)
        assertEquals(emptyMap(), ladder.round?.outcome?.points)
        assertEquals(SubmissionId(0), ladder.round?.bracket?.champion)
    }

    @Test
    fun `an odd table walks its odd answer through without handing it a point`() {
        val three = ladderOf(GameFixtures.players("alice", "bob", "carl"))
        val bracket = assertNotNull(three.round?.bracket)

        assertEquals(2, bracket.duels.size)
        assertTrue(bracket.duels[1].isBye)
        assertEquals(SubmissionId(2), bracket.duels[1].winner)
        assertEquals(0, bracket.winsOf(SubmissionId(2)), "a walkover is not a win")
    }

    @Test
    fun `there is nothing to judge when a single answer came in`() {
        val alone = ladderOf(GameFixtures.players("alice", "bob"), answering = 1)

        // One answer, no duel: the ladder is settled the moment it opens, and the round
        // is scored rather than left waiting on a vote nobody can cast.
        assertEquals(GamePhase.ROUND_RESULT, alone.phase)
        assertNull(alone.round?.outcome?.points?.get(alice))
    }

    private fun startedGame(seated: List<fr.ftnl.cardgame.domain.player.Player> = players): GameState {
        val settings = GameSettings(selectionFormat = SelectionFormat.DUELS, minPlayers = 2)
        val lobby = GameFixtures.lobby(seated, settings)
        val host = seated.first().id
        val withCards = engine.perform(lobby, GameCommand.SetCardPool(host, GameFixtures.pool()))
        return engine.perform(withCards, GameCommand.Start(host))
    }

    private fun everybodyAnswers(): GameState = ladderOf(players)

    /** Deals a round and has the first [answering] players play, closing the step. */
    private fun ladderOf(
        seated: List<fr.ftnl.cardgame.domain.player.Player>,
        answering: Int = seated.size,
    ): GameState {
        val running = startedGame(seated)
        val played = seated.take(answering).foldIndexed(running) { index, state, player ->
            engine.perform(state, GameCommand.PlayCards(player.id, listOf(CardId("p${index * 10 + 1}"))))
        }
        // Not everybody played, so the timer is what ends the answering step.
        return if (answering == seated.size) played
        else engine.perform(played, GameCommand.CloseSubmissions)
    }

    /** alice beats bob, unanimously among the two players who may say so. */
    private fun duelOne(): GameState = listOf(carl, dave)
        .fold(answered) { state, voter -> engine.perform(state, GameCommand.Choose(voter, SubmissionId(0))) }

    /** Then carl beats dave, and alice takes the final. */
    private fun wholeLadder(): GameState {
        val second = listOf(alice, bob)
            .fold(duelOne()) { state, voter ->
                engine.perform(state, GameCommand.Choose(voter, SubmissionId(2)))
            }
        return listOf(bob, dave)
            .fold(second) { state, voter -> engine.perform(state, GameCommand.Choose(voter, SubmissionId(0))) }
    }

}
