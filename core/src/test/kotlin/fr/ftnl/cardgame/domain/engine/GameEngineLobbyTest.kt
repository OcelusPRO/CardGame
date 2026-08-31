package fr.ftnl.cardgame.domain.engine

import fr.ftnl.cardgame.domain.card.CardPool
import fr.ftnl.cardgame.domain.game.GamePhase
import fr.ftnl.cardgame.domain.game.GameSettings
import fr.ftnl.cardgame.domain.player.Nickname
import fr.ftnl.cardgame.domain.player.PlayerId
import fr.ftnl.cardgame.domain.support.GameFixtures
import fr.ftnl.cardgame.domain.support.eventsOf
import fr.ftnl.cardgame.domain.support.perform
import fr.ftnl.cardgame.domain.support.refusal
import fr.ftnl.cardgame.domain.support.testEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameEngineLobbyTest {

    private val engine = testEngine()
    private val alice = GameFixtures.player("alice")
    private val bob = GameFixtures.player("bob")
    private val lobby = GameFixtures.lobby(listOf(alice))

    @Test
    fun `a new player takes a seat with a zero score`() {
        val state = engine.perform(lobby, GameCommand.Join(bob))

        assertEquals(listOf("alice", "bob"), state.players.map { it.nickname.value })
        assertEquals(0, state.scoreboard.pointsOf(bob.id))
    }

    @Test
    fun `joining twice brings the player back online instead of duplicating the seat`() {
        val gone = engine.perform(lobby, GameCommand.SetConnected(alice.id, connected = false))

        val back = engine.perform(gone, GameCommand.Join(alice))

        assertEquals(1, back.players.size)
        assertTrue(back.players.single().connected)
    }

    @Test
    fun `refuses a nickname already at the table`() {
        val twin = bob.copy(id = PlayerId("other"), nickname = alice.nickname)

        assertEquals(GameError.NICKNAME_TAKEN, engine.refusal(lobby, GameCommand.Join(twin)))
    }

    @Test
    fun `refuses a player once the table is full`() {
        val full = lobby.copy(settings = GameSettings(minPlayers = 2, maxPlayers = 2), players = listOf(alice, bob))

        val newcomer = GameFixtures.player("carl")

        assertEquals(GameError.GAME_FULL, engine.refusal(full, GameCommand.Join(newcomer)))
    }

    @Test
    fun `leaving the lobby frees the seat and hands over the crown`() {
        val table = engine.perform(lobby, GameCommand.Join(bob))

        val state = engine.perform(table, GameCommand.Leave(alice.id))

        assertEquals(listOf(bob.id), state.players.map { it.id })
        assertEquals(bob.id, state.hostId)
    }

    @Test
    fun `only the host may change the settings`() {
        val table = engine.perform(lobby, GameCommand.Join(bob))

        val refusal = engine.refusal(table, GameCommand.UpdateSettings(bob.id, GameSettings()))

        assertEquals(GameError.NOT_THE_HOST, refusal)
    }

    @Test
    fun `the host may not kick themselves`() {
        val table = engine.perform(lobby, GameCommand.Join(bob))

        assertEquals(GameError.CANNOT_KICK_SELF, engine.refusal(table, GameCommand.Kick(alice.id, alice.id)))
    }

    @Test
    fun `starting needs enough players`() {
        val withCards = engine.perform(lobby, GameCommand.SetCardPool(alice.id, GameFixtures.pool()))

        assertEquals(GameError.NOT_ENOUGH_PLAYERS, engine.refusal(withCards, GameCommand.Start(alice.id)))
    }

    @Test
    fun `starting needs a situation to play`() {
        val table = engine.perform(lobby, GameCommand.Join(bob))
        val empty = engine.perform(table, GameCommand.SetCardPool(alice.id, CardPool.EMPTY))

        assertEquals(GameError.EMPTY_DECK, engine.refusal(empty, GameCommand.Start(alice.id)))
    }

    @Test
    fun `starting needs enough punchlines to fill every hand`() {
        val table = engine.perform(lobby, GameCommand.Join(bob))
        val thin = engine.perform(table, GameCommand.SetCardPool(alice.id, GameFixtures.pool(punchlines = 5)))

        assertEquals(GameError.NOT_ENOUGH_CARDS, engine.refusal(thin, GameCommand.Start(alice.id)))
    }

    @Test
    fun `settings are frozen once the game runs`() {
        val running = startedGame()

        val refusal = engine.refusal(running, GameCommand.UpdateSettings(alice.id, GameSettings()))

        assertEquals(GameError.WRONG_PHASE, refusal)
    }

    @Test
    fun `starting deals a full hand to everybody and opens the answering step`() {
        val running = startedGame()

        assertEquals(GamePhase.SUBMITTING, running.phase)
        assertEquals(running.settings.handSize, running.handOf(alice.id).size)
        assertEquals(running.settings.handSize, running.handOf(bob.id).size)
        assertFalse(running.round?.situation?.id?.value.isNullOrBlank())
    }

    @Test
    fun `starting reports every official card that landed in a hand`() {
        val ready = engine.perform(
            engine.perform(lobby, GameCommand.Join(bob)),
            GameCommand.SetCardPool(alice.id, GameFixtures.pool()),
        )

        val events = engine.eventsOf(ready, GameCommand.Start(alice.id))

        val refilled = events.filterIsInstance<GameEvent.HandsRefilled>().single()
        assertEquals(2 * GameFixtures.duoFriendly().handSize, refilled.punchlineCardIds.size)
        assertEquals(refilled.punchlineCardIds.size, refilled.punchlineCardIds.distinct().size)
    }

    @Test
    fun `a late player cannot join a running game`() {
        val running = startedGame()
        val latecomer = GameFixtures.player("carl")

        assertEquals(GameError.GAME_ALREADY_STARTED, engine.refusal(running, GameCommand.Join(latecomer)))
    }

    @Test
    fun `a nickname is normalised before the duplicate check`() {
        val spaced = bob.copy(id = PlayerId("other"), nickname = Nickname.of("  alice "))

        assertEquals(GameError.NICKNAME_TAKEN, engine.refusal(lobby, GameCommand.Join(spaced)))
    }

    private fun startedGame() = engine.perform(
        engine.perform(
            engine.perform(lobby, GameCommand.Join(bob)),
            GameCommand.SetCardPool(alice.id, GameFixtures.pool()),
        ),
        GameCommand.Start(alice.id),
    )
}
