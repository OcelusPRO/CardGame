package fr.ftnl.cardgame.domain.game

import fr.ftnl.cardgame.domain.card.CardId
import fr.ftnl.cardgame.domain.engine.GameCommand
import fr.ftnl.cardgame.domain.support.GameFixtures
import fr.ftnl.cardgame.domain.support.perform
import fr.ftnl.cardgame.domain.support.testEngine
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/** A running game is snapshotted into Redis, so it must survive a JSON round trip untouched. */
class GameStateSerializationTest {

    private val json = Json
    private val engine = testEngine()
    private val players = GameFixtures.players("alice", "bob", "carl")

    @Test
    fun `a mid round snapshot survives a JSON round trip`() {
        val state = midRound()

        val restored = json.decodeFromString(GameState.serializer(), json.encodeToString(GameState.serializer(), state))

        assertEquals(state, restored)
    }

    @Test
    fun `a lobby snapshot survives a JSON round trip`() {
        val lobby = GameFixtures.lobby(players)

        val restored = json.decodeFromString(GameState.serializer(), json.encodeToString(GameState.serializer(), lobby))

        assertEquals(lobby, restored)
    }

    private fun midRound(): GameState {
        val lobby = GameFixtures.lobby(players)
        val withCards = engine.perform(lobby, GameCommand.SetCardPool(players[0].id, GameFixtures.pool()))
        val running = engine.perform(withCards, GameCommand.Start(players[0].id))
        return engine.perform(running, GameCommand.PlayCards(players[0].id, listOf(CardId("p1"))))
    }
}
