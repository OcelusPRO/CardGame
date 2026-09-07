package fr.ftnl.cardgame.ws

import fr.ftnl.cardgame.domain.game.GameCode
import java.util.concurrent.ConcurrentHashMap

/** Which sockets are currently watching which game, and the live counters that follow. */
class GameConnections {

    private val byGame = ConcurrentHashMap<String, MutableSet<TableConnection>>()

    fun add(connection: TableConnection) {
        byGame.computeIfAbsent(connection.code.value) { ConcurrentHashMap.newKeySet() } += connection
    }

    fun remove(connection: TableConnection) {
        val remaining = byGame[connection.code.value] ?: return
        remaining -= connection
        if (remaining.isEmpty()) byGame.remove(connection.code.value)
    }

    /** Everybody watching, players and spectators alike — who the broadcast goes out to. */
    fun of(code: GameCode): List<TableConnection> = byGame[code.value].orEmpty().toList()

    /** The seats alone. A table full of spectators and no players is a table nobody is at. */
    fun playersOf(code: GameCode): List<GameConnection> = of(code).filterIsInstance<GameConnection>()

    /** Number of games with at least one socket watching, shown live in the admin. */
    fun activeGames(): Int = byGame.size

    /** Sockets held by an actual player; the stream pages are deliberately left out. */
    fun connectedPlayers(): Int =
        byGame.values.sumOf { sockets -> sockets.count { it is GameConnection } }

    /**
     * Sends the watchers of a dead table home. Without this a stream page left open would
     * hold a snapshot of a game that no longer exists, frozen and reconnecting forever.
     */
    suspend fun closeSpectators(code: GameCode, reason: String) {
        of(code).filterIsInstance<SpectatorConnection>().forEach { it.close(reason) }
    }
}
