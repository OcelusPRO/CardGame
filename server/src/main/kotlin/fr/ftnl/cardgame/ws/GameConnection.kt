package fr.ftnl.cardgame.ws

import fr.ftnl.cardgame.domain.game.GameCode
import fr.ftnl.cardgame.domain.player.PlayerId
import io.ktor.websocket.CloseReason
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.send
import kotlinx.serialization.json.Json

/**
 * One open socket on a table.
 *
 * There are two kinds, and the difference is not cosmetic: a [GameConnection] is a seat —
 * it holds a hand, it sends commands, and the table is deserted when the last one goes —
 * while a [SpectatorConnection] is a pair of eyes on the stream page. Everything that
 * counts players, keeps the game alive or projects a private view has to ask which one it
 * is looking at, which is why the two are types rather than a boolean.
 */
sealed class TableConnection(
    val code: GameCode,
    private val session: WebSocketSession,
    private val json: Json,
) {
    /** Swallows a send failure: a dead socket is cleaned up by its own read loop. */
    suspend fun send(message: ServerMessage) {
        runCatching { session.send(json.encodeToString(ServerMessage.serializer(), message)) }
    }

    /** Shuts the socket for a reason of ours, rather than waiting for the client to notice. */
    suspend fun close(reason: String) {
        runCatching { session.close(CloseReason(CloseReason.Codes.NORMAL, reason)) }
    }
}

/** A socket held by somebody sitting at the table, tied to the player it authenticated as. */
class GameConnection(
    code: GameCode,
    val playerId: PlayerId,
    session: WebSocketSession,
    json: Json,
) : TableConnection(code, session, json)

/**
 * A socket that only watches: the page a streamer puts on screen, or leaves open on a
 * second monitor. It carries no identity — the link is the whole credential — so it is
 * never dealt a hand, never counted as a player, and never allowed to send a command.
 */
class SpectatorConnection(
    code: GameCode,
    session: WebSocketSession,
    json: Json,
) : TableConnection(code, session, json)
