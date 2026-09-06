package fr.ftnl.cardgame.domain.engine

import fr.ftnl.cardgame.domain.game.ChatVoteTally
import fr.ftnl.cardgame.domain.game.GamePhase
import fr.ftnl.cardgame.domain.game.GameSettings
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.game.SelectionFormat
import fr.ftnl.cardgame.domain.game.SelectionMode
import fr.ftnl.cardgame.domain.game.SubmissionId
import fr.ftnl.cardgame.domain.player.Player
import fr.ftnl.cardgame.domain.support.GameFixtures
import fr.ftnl.cardgame.domain.support.perform
import fr.ftnl.cardgame.domain.support.refusal
import fr.ftnl.cardgame.domain.support.testEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The duel format against each of the three ways of deciding who judges.
 *
 * They are separate settings on purpose — one says **who** votes, the other **how** — so
 * what matters here is that the ladder does not care about the answer to the first
 * question, and that the first question keeps its own rules when the ladder is running.
 */
class GameEngineDuelFormatTest {

    private val engine = testEngine()
    private val players = GameFixtures.players("alice", "bob", "carl", "dave")
    private val alice = players[0].id
    private val bob = players[1].id
    private val carl = players[2].id
    private val dave = players[3].id

    // --- everybody votes, two answers at a time ---------------------------------------

    @Test
    fun `with everybody voting, a duel is settled by the table minus its two authors`() {
        val open = duelling(SelectionMode.VOTE)

        val voted = engine.perform(open, GameCommand.Choose(carl, SubmissionId(1)))
        val settled = engine.perform(voted, GameCommand.Choose(dave, SubmissionId(1)))

        assertEquals(1, settled.round?.bracket?.winsOf(SubmissionId(1)))
        assertEquals(GamePhase.SELECTING, settled.phase, "the ladder is not over yet")
    }

    // --- a rotating czar, two answers at a time ---------------------------------------

    @Test
    fun `with a czar, the czar alone settles each duel`() {
        val open = duelling(SelectionMode.CZAR)
        val czar = assertNotNull(open.round?.czarId)

        val other = players.map { it.id }.first { it != czar }
        assertEquals(GameError.NOT_THE_CZAR, engine.refusal(open, GameCommand.Choose(other, SubmissionId(0))))

        val settled = engine.perform(open, GameCommand.Choose(czar, SubmissionId(0)))
        assertEquals(1, settled.round?.bracket?.winsOf(SubmissionId(0)))
    }

    @Test
    fun `a czar ruling on duels still never picks their own answer`() {
        val settings = duelSettings(SelectionMode.CZAR).copy(czarAnswers = true, allowSelfVote = true)
        val open = duelling(settings = settings)
        val round = assertNotNull(open.round)
        val czar = assertNotNull(round.czarId)
        val own = assertNotNull(round.handleOf(czar))
        val duel = assertNotNull(round.bracket?.current)

        // A czar who answers can end up facing their own card. Self voting is on for this
        // table, and it still does not buy them the right to crown themselves.
        if (duel.holds(own)) {
            assertEquals(
                GameError.CANNOT_VOTE_OWN_ANSWER,
                engine.refusal(open, GameCommand.Choose(czar, own)),
            )
        } else {
            assertEquals(1, engine.perform(open, GameCommand.Choose(czar, duel.left)).round
                ?.bracket?.winsOf(duel.left))
        }
    }

    // --- the Twitch chat, two answers at a time ----------------------------------------

    @Test
    fun `with the chat judging, the table is still barred from every duel`() {
        val open = duelling(SelectionMode.CHAT)

        assertEquals(
            GameError.ONLY_THE_CHAT_VOTES,
            engine.refusal(open, GameCommand.Choose(carl, SubmissionId(0))),
        )
    }

    @Test
    fun `a duel the chat decided is won by the side the viewers picked`() {
        val open = duelling(SelectionMode.CHAT)

        // The scheduler banks the tally, then closes: exactly what happens in production.
        val counted = engine.perform(open, GameCommand.SetChatVotes(chat(SubmissionId(1) to 40, SubmissionId(0) to 12)))
        val settled = engine.perform(counted, GameCommand.CloseSelection)

        val bracket = assertNotNull(settled.round?.bracket)
        assertEquals(1, bracket.winsOf(SubmissionId(1)), "the viewers' side should have taken the duel")
        assertEquals(0, bracket.winsOf(SubmissionId(0)))
    }

    @Test
    fun `the voices of a settled duel do not decide the next one`() {
        val open = duelling(SelectionMode.CHAT)
        val counted = engine.perform(open, GameCommand.SetChatVotes(chat(SubmissionId(0) to 40)))

        val next = engine.perform(counted, GameCommand.CloseSelection)

        assertEquals(emptyMap(), next.round?.chatVotes, "the tally should have been spent")
        assertEquals(SubmissionId(2), next.round?.bracket?.current?.left)
        assertEquals(SubmissionId(3), next.round?.bracket?.current?.right)
    }

    @Test
    fun `a chat ladder pays by the duel, not by the size of the audience`() {
        val ladder = generateSequence(duelling(SelectionMode.CHAT)) { state ->
            state.takeIf { it.phase == GamePhase.SELECTING }?.let {
                // Four thousand viewers on one side, every single duel.
                val leading = it.round?.bracket?.current?.left ?: return@let null
                val counted = engine.perform(it, GameCommand.SetChatVotes(chat(leading to 4_000)))
                engine.perform(counted, GameCommand.CloseSelection)
            }
        }.last()

        assertEquals(GamePhase.ROUND_RESULT, ladder.phase)
        // Two duels won by the same answer: two points, not eight thousand.
        assertEquals(2, ladder.scoreboard.pointsOf(alice))
        assertTrue(ladder.scoreboard.pointsOf(bob) == 0)
    }

    // --- fixtures ---------------------------------------------------------------------

    private fun duelSettings(mode: SelectionMode) = GameSettings(
        selectionMode = mode,
        selectionFormat = SelectionFormat.DUELS,
        minPlayers = 2,
    )

    /** A round taken to the point where the first duel is on the table. */
    private fun duelling(
        mode: SelectionMode = SelectionMode.VOTE,
        settings: GameSettings = duelSettings(mode),
    ): GameState {
        val lobby = GameFixtures.lobby(seated(settings), settings)
        val withCards = engine.perform(lobby, GameCommand.SetCardPool(alice, GameFixtures.pool()))
        val started = engine.perform(withCards, GameCommand.Start(alice))
        // Each player plays whatever the deal put in front of them — in the czar mode one
        // of them holds no hand at all, so the card cannot be named ahead of time.
        val answered = started.answeringPlayers.fold(started) { state, player ->
            val card = state.handOf(player.id).firstOrNull() ?: return@fold state
            engine.perform(state, GameCommand.PlayCards(player.id, listOf(card.id)))
        }
        // In the czar mode one player sits the round out, so the step needs the timer.
        return if (answered.phase == GamePhase.SELECTING) answered
        else engine.perform(answered, GameCommand.CloseSubmissions)
    }

    /**
     * The chat mode reads a channel off the host, and there is no chat to read without
     * one — the ladder would never see a voice.
     */
    private fun seated(settings: GameSettings): List<Player> =
        if (settings.selectionMode == SelectionMode.CHAT) {
            players.mapIndexed { index, player ->
                if (index == 0) player.copy(twitchLogin = "kameto") else player
            }
        } else {
            players
        }

    private fun chat(vararg counts: Pair<SubmissionId, Int>): Map<SubmissionId, ChatVoteTally> =
        counts.associate { (id, count) -> id to ChatVoteTally(count = count) }
}
