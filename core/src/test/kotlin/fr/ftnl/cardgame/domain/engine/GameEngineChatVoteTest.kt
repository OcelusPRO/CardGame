package fr.ftnl.cardgame.domain.engine

import fr.ftnl.cardgame.domain.card.CardId
import fr.ftnl.cardgame.domain.game.ChatVoteTally
import fr.ftnl.cardgame.domain.game.ChatVoter
import fr.ftnl.cardgame.domain.game.GamePhase
import fr.ftnl.cardgame.domain.game.GameSettings
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.game.SelectionMode
import fr.ftnl.cardgame.domain.game.SubmissionId
import fr.ftnl.cardgame.domain.support.FixedClock
import fr.ftnl.cardgame.domain.support.GameFixtures
import fr.ftnl.cardgame.domain.support.perform
import fr.ftnl.cardgame.domain.support.refusal
import fr.ftnl.cardgame.domain.support.testEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The chat of a streaming table: which chats are read, and what the engine accepts. */
class GameEngineChatVoteTest {

    private val engine = testEngine(FixedClock(1_000_000))
    private val players = GameFixtures.players("alice", "bob", "carl").mapIndexed { index, player ->
        when (index) {
            0 -> player.copy(twitchLogin = "kameto")
            1 -> player.copy(twitchLogin = "ponce")
            else -> player
        }
    }
    private val alice = players[0].id
    private val bob = players[1].id
    private val carl = players[2].id

    @Test
    fun `no chat is read until the host asks for it`() {
        assertTrue(lobby(GameFixtures.duoFriendly()).chatChannels.isEmpty())
    }

    @Test
    fun `the host's chat is read once the option is on`() {
        assertEquals(listOf("kameto"), lobby(chatVote()).chatChannels)
    }

    @Test
    fun `the other streamers only join when the host includes them`() {
        val together = chatVote().copy(twitchGuestChats = true)

        assertEquals(listOf("kameto", "ponce"), lobby(together).chatChannels)
    }

    @Test
    fun `a czar deciding alone leaves no room for a chat`() {
        val czar = GameFixtures.duoFriendly().copy(selectionMode = SelectionMode.CZAR)

        assertTrue(lobby(czar).chatChannels.isEmpty())
    }

    @Test
    fun `nobody at the table votes once the chat judges`() {
        val voting = everybodyAnswers(chatVote())
        val handle = voting.round?.handleOf(bob)!!

        assertEquals(
            GameError.ONLY_THE_CHAT_VOTES,
            engine.refusal(voting, GameCommand.Choose(alice, handle)),
        )
    }

    @Test
    fun `the chat alone hands out the points, one voice per viewer`() {
        val voting = everybodyAnswers(chatVote())
        val forAlice = voting.round?.handleOf(alice)!!
        val counted = engine.perform(voting, GameCommand.SetChatVotes(mapOf(forAlice to tally(240))))

        val scored = engine.perform(counted, GameCommand.CloseSelection)

        assertEquals(GamePhase.ROUND_RESULT, scored.phase)
        // Every viewer went the same way, so the answer also takes the unanimity bonus.
        val expected = 240 * scored.settings.scoring.pointsPerVote + scored.settings.scoring.unanimityBonus
        assertEquals(expected, scored.scoreboard.pointsOf(alice))
        assertEquals(0, scored.scoreboard.pointsOf(bob))
    }

    @Test
    fun `a player who signs in after sitting down still brings their chat`() {
        val linked = engine.perform(lobby(chatVote()), GameCommand.LinkTwitch(carl, "zerator"))

        assertEquals("zerator", linked.playerOf(carl)?.twitchLogin)
    }

    @Test
    fun `the tally read from the chats lands on the round, faces included`() {
        val voting = everybodyAnswers(chatVote())
        val tallies = mapOf(SubmissionId(0) to tally(12, faces = 3))

        val counted = engine.perform(voting, GameCommand.SetChatVotes(tallies))

        val landed = counted.round?.chatVotes?.get(SubmissionId(0))
        assertEquals(12, landed?.count)
        assertEquals(listOf("Viewer 1", "Viewer 2", "Viewer 3"), landed?.voters?.map { it.name })
    }

    @Test
    fun `an answer number the viewers made up is dropped`() {
        val voting = everybodyAnswers(chatVote())
        val tallies = mapOf(SubmissionId(0) to tally(3), SubmissionId(8) to tally(40))

        val counted = engine.perform(voting, GameCommand.SetChatVotes(tallies))

        assertEquals(setOf(SubmissionId(0)), counted.round?.chatVotes?.keys)
    }

    @Test
    fun `nothing is counted outside the judging step`() {
        val answering = started(chatVote())

        assertEquals(
            GameError.WRONG_PHASE,
            engine.refusal(answering, GameCommand.SetChatVotes(mapOf(SubmissionId(0) to tally(1)))),
        )
    }

    @Test
    fun `nothing is counted on a table no chat is watching`() {
        val voting = everybodyAnswers(GameFixtures.duoFriendly())

        assertEquals(
            GameError.CHAT_VOTE_CLOSED,
            engine.refusal(voting, GameCommand.SetChatVotes(mapOf(SubmissionId(0) to tally(1)))),
        )
    }

    @Test
    fun `the timer alone closes a round the chat is judging`() {
        val voting = everybodyAnswers(chatVote())

        // There is nobody left to wait for at the table, and the step stays open anyway.
        assertEquals(GamePhase.SELECTING, voting.phase)
        assertEquals(GamePhase.ROUND_RESULT, engine.perform(voting, GameCommand.CloseSelection).phase)
    }

    private fun chatVote(): GameSettings =
        GameFixtures.duoFriendly().copy(selectionMode = SelectionMode.CHAT)

    private fun tally(count: Int, faces: Int = 0) = ChatVoteTally(
        count = count,
        voters = (1..minOf(faces, count)).map { ChatVoter("v$it", "Viewer $it") },
    )

    private fun lobby(settings: GameSettings): GameState = GameFixtures.lobby(players, settings)

    private fun started(settings: GameSettings): GameState {
        val withCards = engine.perform(lobby(settings), GameCommand.SetCardPool(alice, GameFixtures.pool()))
        return engine.perform(withCards, GameCommand.Start(alice))
    }

    private fun everybodyAnswers(settings: GameSettings): GameState =
        listOf(alice to "p1", bob to "p11", carl to "p21")
            .fold(started(settings)) { state, (player, card) ->
                engine.perform(state, GameCommand.PlayCards(player, listOf(CardId(card))))
            }
}
