package fr.ftnl.cardgame.api.view

import fr.ftnl.cardgame.domain.card.CardId
import fr.ftnl.cardgame.domain.card.CardOrigin
import fr.ftnl.cardgame.domain.card.CardPool
import fr.ftnl.cardgame.domain.card.PunchlineCard
import fr.ftnl.cardgame.domain.card.SituationCard
import fr.ftnl.cardgame.domain.card.SituationText
import fr.ftnl.cardgame.domain.deck.IdentityShuffler
import fr.ftnl.cardgame.domain.engine.CommandResult
import fr.ftnl.cardgame.domain.engine.GameCommand
import fr.ftnl.cardgame.domain.engine.GameEngine
import fr.ftnl.cardgame.domain.game.GameClock
import fr.ftnl.cardgame.domain.game.GameCode
import fr.ftnl.cardgame.domain.game.GameSettings
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.game.Scoreboard
import fr.ftnl.cardgame.domain.player.Avatar
import fr.ftnl.cardgame.domain.player.AvatarPart
import fr.ftnl.cardgame.domain.player.Nickname
import fr.ftnl.cardgame.domain.player.Player
import fr.ftnl.cardgame.domain.player.PlayerId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The projection is the only thing standing between a player and the answers of the
 * others, so its hiding rules are tested step by step.
 */
class GameViewFactoryTest {

    private val clock = GameClock { 5_000 }
    private val engine = GameEngine(IdentityShuffler, clock)
    private val factory = GameViewFactory(clock)

    private val alice = player("alice")
    private val bob = player("bob")
    private val carl = player("carl")

    @Test
    fun `a player only ever receives their own hand`() {
        val view = factory.create(running(), alice.id)

        assertEquals(running().handOf(alice.id).size, view.you.hand.size)
        assertTrue(view.players.all { it.score == 0 })
    }

    @Test
    fun `no answer leaks while the others are still choosing`() {
        val answered = engine.run(running(), GameCommand.PlayCards(alice.id, listOf(CardId("p1"))))

        val view = factory.create(answered, bob.id)

        assertTrue(view.round?.answers.isNullOrEmpty())
        assertTrue(view.players.single { it.id == alice.id.value }.hasAnswered)
    }

    @Test
    fun `during the vote the answers are visible but not their authors`() {
        val voting = everybodyAnswers()

        val view = factory.create(voting, bob.id)
        val answers = view.round?.answers.orEmpty()

        assertEquals(3, answers.size)
        assertTrue(answers.all { it.authorId == null })
        assertTrue(answers.all { it.votes == null })
        assertEquals(1, answers.count { it.isMine })
    }

    @Test
    fun `the reveal ties every answer back to its author`() {
        val scored = everybodyVotes()

        val answers = factory.create(scored, bob.id).round?.answers.orEmpty()

        assertTrue(answers.all { it.authorId != null })
        assertTrue(answers.all { it.votes != null })
    }

    @Test
    fun `the answer is rendered inside the situation`() {
        val voting = everybodyAnswers()

        val answer = factory.create(voting, bob.id).round?.answers?.first()

        assertEquals("Le pire, c'est punchline p1.", answer?.filledText)
    }

    @Test
    fun `the viewer is told when the game waits for them`() {
        val view = factory.create(running(), alice.id)

        assertTrue(view.you.mustAnswer)
        assertFalse(view.you.mustVote)
    }

    @Test
    fun `a custom card is flagged so the client can style it apart`() {
        val custom = SituationCard(CardId("c1"), SituationText("Chez moi, ____."), CardOrigin.CUSTOM)
        val state = running().let { it.copy(round = it.round?.copy(situation = custom)) }

        assertEquals(true, factory.create(state, alice.id).round?.situation?.custom)
    }

    @Test
    fun `the deadline and the server clock travel together`() {
        val view = factory.create(running(), alice.id)

        assertEquals(5_000, view.serverTimeMillis)
        assertEquals(5_000 + running().settings.submitSeconds * 1000L, view.deadlineMillis)
    }

    @Test
    fun `the lobby carries no round at all`() {
        assertNull(factory.create(lobby(), alice.id).round)
    }

    private fun player(name: String) = Player(
        PlayerId(name),
        Nickname.of(name),
        Avatar(AvatarPart("head-1", "#ffffff"), AvatarPart("body-1", "#000000")),
    )

    private fun pool() = CardPool(
        situations = (1..4).map { SituationCard(CardId("s$it"), SituationText("Le pire, c'est ____.")) },
        punchlines = (1..60).map { PunchlineCard(CardId("p$it"), "punchline p$it") },
    )

    private fun lobby() = GameState(
        code = GameCode.of("ABCDE"),
        hostId = alice.id,
        players = listOf(alice, bob, carl),
        settings = GameSettings(minPlayers = 2),
        scoreboard = Scoreboard(mapOf(alice.id to 0, bob.id to 0, carl.id to 0)),
    )

    private fun running(): GameState {
        val withCards = engine.run(lobby(), GameCommand.SetCardPool(alice.id, pool()))
        return engine.run(withCards, GameCommand.Start(alice.id))
    }

    private fun everybodyAnswers(): GameState =
        listOf(alice.id to "p1", bob.id to "p11", carl.id to "p21")
            .fold(running()) { state, (player, card) ->
                engine.run(state, GameCommand.PlayCards(player, listOf(CardId(card))))
            }

    private fun everybodyVotes(): GameState {
        val voting = everybodyAnswers()
        val handles = listOf(alice.id, bob.id, carl.id).associateWith { voting.round!!.handleOf(it)!! }
        return listOf(bob.id to alice.id, carl.id to alice.id, alice.id to bob.id)
            .fold(voting) { state, (voter, choice) ->
                engine.run(state, GameCommand.Choose(voter, handles.getValue(choice)))
            }
    }

    private fun GameEngine.run(state: GameState, command: GameCommand): GameState =
        (execute(state, command) as CommandResult.Accepted).state
}
