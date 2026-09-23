package fr.ftnl.cardgame.domain.engine

import fr.ftnl.cardgame.domain.support.GameFixtures
import fr.ftnl.cardgame.domain.support.perform
import fr.ftnl.cardgame.domain.support.refusal
import fr.ftnl.cardgame.domain.support.testEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class GameEngineStepAsideTest {

    private val engine = testEngine()
    private val streamer = GameFixtures.player("stream")
    private val guest = GameFixtures.player("guest")
    private val phone = GameFixtures.player("phone")
    private val lobby = GameFixtures.lobby(listOf(streamer, guest, phone))

    @Test
    fun `the host hands the crown to the seat they name and frees their own`() {
        val state = engine.perform(lobby, GameCommand.StepAside(streamer.id, heir = phone.id))

        assertEquals(phone.id, state.hostId)
        assertFalse(state.contains(streamer.id))
        assertEquals(listOf(guest.id, phone.id), state.players.map { it.id })
    }

    @Test
    fun `the host may not step aside without an heir at the table`() {
        val alone = GameFixtures.lobby(listOf(streamer))

        assertEquals(GameError.UNKNOWN_PLAYER, engine.refusal(alone, GameCommand.StepAside(streamer.id)))
        assertEquals(
            GameError.UNKNOWN_PLAYER,
            engine.refusal(lobby, GameCommand.StepAside(streamer.id, heir = streamer.id)),
        )
    }

    @Test
    fun `a guest steps aside without touching the crown`() {
        val state = engine.perform(lobby, GameCommand.StepAside(guest.id, heir = phone.id))

        assertEquals(streamer.id, state.hostId)
        assertFalse(state.contains(guest.id))
    }

    @Test
    fun `nobody steps aside once the match is under way`() {
        val withCards = engine.perform(lobby, GameCommand.SetCardPool(streamer.id, GameFixtures.pool()))
        val running = engine.perform(withCards, GameCommand.Start(streamer.id))

        assertEquals(
            GameError.WRONG_PHASE,
            engine.refusal(running, GameCommand.StepAside(streamer.id, heir = phone.id)),
        )
    }
}
