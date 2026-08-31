package fr.ftnl.cardgame.ws

import fr.ftnl.cardgame.domain.game.GameCode
import java.util.concurrent.ConcurrentHashMap

/** Which sockets are currently watching which game, and the live counters that follow. */
class GameConnections {

    private val byGame = ConcurrentHashMap<String, MutableSet<GameConnection>>()

    fun add(connection: GameConnection) {
        byGame.computeIfAbsent(connection.code.value) { ConcurrentHashMap.newKeySet() } += connection
    }

    fun remove(connection: GameConnection) {
        val remaining = byGame[connection.code.value] ?: return
        remaining -= connection
        if (remaining.isEmpty()) byGame.remove(connection.code.value)
    }

    fun of(code: GameCode): List<GameConnection> = byGame[code.value].orEmpty().toList()

    /** Number of games with at least one player watching, shown live in the admin. */
    fun activeGames(): Int = byGame.size

    fun connectedPlayers(): Int = byGame.values.sumOf { it.size }
}
