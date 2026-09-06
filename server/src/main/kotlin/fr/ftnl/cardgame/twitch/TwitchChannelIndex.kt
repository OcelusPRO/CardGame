package fr.ftnl.cardgame.twitch

import fr.ftnl.cardgame.domain.engine.GameEvent
import fr.ftnl.cardgame.domain.game.GameCode
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.game.GameListener
import java.util.concurrent.ConcurrentHashMap

/**
 * Which table a Twitch channel is playing at.
 *
 * The extension panel knows exactly one thing about where it is running: the channel id
 * Twitch signed into the viewer token. That has to become a game code before anything can
 * be done with it, and only the games themselves know — so this follows them.
 *
 * A channel is indexed by **account id**, never by name: a channel can be renamed, and a
 * freed name can be taken by somebody else. Only the host's own channel is indexed, even
 * when the table reads the chats of several streamers — the extension is installed on one
 * channel, and pointing a guest's panel at a table they do not run is not the same thing.
 */
class TwitchChannelIndex : GameListener {

    private val byChannel = ConcurrentHashMap<String, String>()

    override suspend fun onGameCreated(state: GameState) = index(state)

    override suspend fun onGameChanged(state: GameState, events: List<GameEvent>) = index(state)

    override suspend fun onGameForgotten(code: GameCode) {
        byChannel.entries.removeIf { it.value == code.value }
    }

    fun gameOf(channelId: String): GameCode? = byChannel[channelId]?.let(GameCode::ofOrNull)

    val size: Int get() = byChannel.size

    private fun index(state: GameState) {
        val channelId = state.playerOf(state.hostId)?.twitchId
        // The crown moves when a host walks out mid-match, so the old entry has to go
        // with it: a panel must never end up pointing at a table its channel left.
        byChannel.entries.removeIf { it.value == state.code.value && it.key != channelId }
        if (channelId != null) byChannel[channelId] = state.code.value
    }
}
