package fr.ftnl.cardgame.domain.engine

import fr.ftnl.cardgame.domain.card.CardId
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
        val czar = chatVote().copy(selectionMode = SelectionMode.CZAR)

        assertTrue(lobby(czar).chatChannels.isEmpty())
    }

    @Test
    fun `a player who signs in after sitting down still brings their chat`() {
        val linked = engine.perform(lobby(chatVote()), GameCommand.LinkTwitch(carl, "zerator"))

        assertEquals("zerator", linked.playerOf(carl)?.twitchLogin)
    }

    @Test
    fun `the tally read from the chats lands on the round`() {
        val voting = everybodyAnswers(chatVote())
        val tallies = mapOf("kameto" to mapOf(SubmissionId(0) to 12))

        val counted = engine.perform(voting, GameCommand.SetChatVotes(tallies))

        assertEquals(mapOf(SubmissionId(0) to 12), counted.round?.chatVotes?.get("kameto"))
    }

    @Test
    fun `a channel nobody at the table streams on is ignored`() {
        val voting = everybodyAnswers(chatVote())
        val tallies = mapOf("someone-else" to mapOf(SubmissionId(0) to 9_000))

        val counted = engine.perform(voting, GameCommand.SetChatVotes(tallies))

        assertTrue(counted.round?.chatVotes.orEmpty().isEmpty())
    }

    @Test
    fun `an answer number the viewers made up is dropped`() {
        val voting = everybodyAnswers(chatVote())
        val tallies = mapOf("kameto" to mapOf(SubmissionId(0) to 3, SubmissionId(8) to 40))

        val counted = engine.perform(voting, GameCommand.SetChatVotes(tallies))

        assertEquals(mapOf(SubmissionId(0) to 3), counted.round?.chatVotes?.get("kameto"))
    }

    @Test
    fun `nothing is counted outside the judging step`() {
        val answering = started(chatVote())

        assertEquals(
            GameError.WRONG_PHASE,
            engine.refusal(answering, GameCommand.SetChatVotes(mapOf("kameto" to mapOf(SubmissionId(0) to 1)))),
        )
    }

    @Test
    fun `nothing is counted on a table no chat is watching`() {
        val voting = everybodyAnswers(GameFixtures.duoFriendly())

        assertEquals(
            GameError.CHAT_VOTE_CLOSED,
            engine.refusal(voting, GameCommand.SetChatVotes(mapOf("kameto" to mapOf(SubmissionId(0) to 1)))),
        )
    }

    @Test
    fun `the timer alone closes a vote the chat takes part in`() {
        val voting = everybodyAnswers(chatVote())
        val handles = listOf(alice, bob, carl).associateWith { voting.round?.handleOf(it) }

        // Every player has voted, and the step still waits for the viewers.
        val allVoted = listOf(alice to bob, bob to carl, carl to alice)
            .fold(voting) { state, (voter, choice) ->
                engine.perform(state, GameCommand.Choose(voter, handles.getValue(choice)!!))
            }

        assertEquals(GamePhase.SELECTING, allVoted.phase)
        assertEquals(GamePhase.ROUND_RESULT, engine.perform(allVoted, GameCommand.CloseSelection).phase)
    }

    private fun chatVote(): GameSettings = GameFixtures.duoFriendly().copy(twitchChatVote = true)

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
