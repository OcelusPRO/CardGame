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
        handler.serve(this, code, player)
    }
}

/**
 * The stream feed of a table: same game, no seat.
 *
 * There is no session check here, and that is the point. The page behind it is opened by a
 * browser source in OBS and by whoever the streamer sent the link to — neither has a
 * cookie, and asking them to sign in to watch a stream would make the feature unusable.
 * The game code is the credential, exactly as it is for the invitation link, and what
 * comes back is projected by [GameViewFactory.spectate]: no hand, no seat, no commands.
 */
fun Route.spectatorSocketRoute(games: GameService, handler: SpectatorSocketHandler) {
    webSocket("/ws/spectate/{code}") {
        val code = GameCode.ofOrNull(call.parameters["code"].orEmpty())
            ?: return@webSocket reject("Code de partie invalide")
        games.find(code) ?: return@webSocket reject("Partie introuvable")
        handler.serve(this, code)
    }
}

private suspend fun io.ktor.websocket.WebSocketSession.reject(reason: String) =
    close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, reason))
