package fr.ftnl.cardgame.domain.engine

import fr.ftnl.cardgame.domain.card.CardId
import fr.ftnl.cardgame.domain.game.GamePhase
import fr.ftnl.cardgame.domain.game.GameSettings
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.game.SelectionMode
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

class GameEngineSharedDeviceTest {

    private val engine = testEngine(FixedClock(1_000_000))
    private val owner = GameFixtures.player("owner")
    private val sofa = GameFixtures.player("sofa")
    private val couch = GameFixtures.player("couch")
    private val lobby = GameFixtures.lobby(listOf(owner)).copy(deviceOwner = owner.id)

    @Test
    fun `the device seats the players sitting around it`() {
        val state = seated()

        assertEquals(listOf(owner.id, sofa.id, couch.id), state.players.map { it.id })
    }

    @Test
    fun `only the device adds a seat, and nobody joins from elsewhere`() {
        val table = seated()
        val stranger = GameFixtures.player("stranger")

        assertEquals(GameError.NOT_THE_HOST, engine.refusal(table, GameCommand.AddSeat(sofa.id, stranger)))
        assertEquals(GameError.SHARED_DEVICE, engine.refusal(table, GameCommand.Join(stranger)))
    }

    @Test
    fun `an online table cannot have seats added for it`() {
        val online = GameFixtures.lobby(listOf(owner))

        assertEquals(GameError.NOT_THE_HOST, engine.refusal(online, GameCommand.AddSeat(owner.id, sofa)))
    }

    @Test
    fun `a dropped socket neither takes anyone offline nor moves the crown`() {
        val state = engine.perform(seated(), GameCommand.SetConnected(owner.id, connected = false))
        val later = engine.perform(state, GameCommand.DropIfAway(owner.id))

        assertTrue(later.players.all { it.connected })
        assertEquals(owner.id, later.hostId)
        assertEquals(3, later.players.size)
    }

    @Test
    fun `answering and voting wait for the phone to go round`() {
        val running = started()
        assertNull(running.phaseDeadlineMillis)

        val judging = answered(running)
        assertEquals(GamePhase.SELECTING, judging.phase)
        assertNull(judging.phaseDeadlineMillis)
    }

    @Test
    fun `the chat still judges against the clock`() {
        val chat = seated().copy(settings = GameSettings(minPlayers = 2, selectionMode = SelectionMode.CHAT))
        val withCards = engine.perform(chat, GameCommand.SetCardPool(owner.id, GameFixtures.pool()))
        val judging = answered(engine.perform(withCards, GameCommand.Start(owner.id)))

        assertNotNull(judging.phaseDeadlineMillis)
    }

    @Test
    fun `the result still moves on by itself`() {
        val judging = answered(started())
        val voted = listOf(owner.id, sofa.id, couch.id).fold(judging) { state, voter ->
            val choice = state.round!!.revealed.first { (_, answer) -> answer.playerId != voter }.first
            engine.perform(state, GameCommand.Choose(voter, choice))
        }

        assertEquals(GamePhase.ROUND_RESULT, voted.phase)
        assertNotNull(voted.phaseDeadlineMillis)
    }

    @Test
    fun `the device leaving takes the whole sofa with it`() {
        val state = engine.perform(seated(), GameCommand.Leave(owner.id))

        assertTrue(state.players.isEmpty())
    }

    @Test
    fun `a shared device has nobody to step aside for`() {
        assertEquals(
            GameError.SHARED_DEVICE,
            engine.refusal(seated(), GameCommand.StepAside(owner.id, heir = sofa.id)),
        )
    }

    private fun seated(): GameState = listOf(sofa, couch).fold(lobby) { state, player ->
        engine.perform(state, GameCommand.AddSeat(owner.id, player))
    }

    private fun started(): GameState {
        val withCards = engine.perform(seated(), GameCommand.SetCardPool(owner.id, GameFixtures.pool()))
        return engine.perform(withCards, GameCommand.Start(owner.id))
    }

    private fun answered(running: GameState): GameState =
        listOf(owner.id to "p1", sofa.id to "p11", couch.id to "p21").fold(running) { state, (player, card) ->
            engine.perform(state, GameCommand.PlayCards(player, listOf(CardId(card))))
        }
}
