package fr.ftnl.cardgame.ws

import fr.ftnl.cardgame.auth.PlayerSession
import fr.ftnl.cardgame.domain.game.GameCode
import fr.ftnl.cardgame.domain.player.PlayerId
import fr.ftnl.cardgame.game.GameService
import io.ktor.server.routing.Route
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close

/** Authenticates the socket from the browser cookie, then hands it to [GameSocketHandler]. */
fun Route.gameSocketRoute(games: GameService, handler: GameSocketHandler) {
    webSocket("/ws/game/{code}") {
        val player = call.sessions.get<PlayerSession>()
            ?: return@webSocket reject("Aucune session de joueur")
        val code = GameCode.ofOrNull(call.parameters["code"].orEmpty())
            ?: return@webSocket reject("Code de partie invalide")
        val playerId = PlayerId(player.playerId)
        val state = games.find(code) ?: return@webSocket reject("Partie introuvable")
        if (!state.contains(playerId)) return@webSocket reject("Vous n'êtes pas à cette table")
        handler.serve(this, code, playerId)
    }
}

private suspend fun io.ktor.websocket.WebSocketSession.reject(reason: String) =
    close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, reason))
