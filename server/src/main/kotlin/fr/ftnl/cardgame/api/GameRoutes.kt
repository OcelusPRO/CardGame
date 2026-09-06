package fr.ftnl.cardgame.api

import fr.ftnl.cardgame.api.dto.CreateGameRequest
import fr.ftnl.cardgame.api.dto.ErrorResponse
import fr.ftnl.cardgame.api.dto.JoinGameRequest
import fr.ftnl.cardgame.auth.playerSession
import fr.ftnl.cardgame.domain.game.GameCode
import fr.ftnl.cardgame.domain.player.PlayerId
import fr.ftnl.cardgame.game.GameEntryService
import fr.ftnl.cardgame.game.JoinOutcome
import fr.ftnl.cardgame.plugins.CREATE_GAMES
import fr.ftnl.cardgame.plugins.JOIN_GAMES
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

/** Creating a table, peeking at one, and taking a seat. Everything else runs on the socket. */
fun Route.gameRoutes(entry: GameEntryService) {
    route("/api/games") {

        // Both of these are open to anyone holding a link, and both cost the server a
        // catalogue read and a snapshot write. They are the only capped routes.
        rateLimit(CREATE_GAMES) {
            post {
                val request = call.receive<CreateGameRequest>()
                call.respond(entry.create(request, call.playerSession(), call.baseUrl()))
            }
        }

        get("{code}") {
            val code = call.gameCode() ?: return@get call.respondUnknownCode()
            val preview = entry.preview(code, PlayerId(call.playerSession().playerId))
                ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("GAME_NOT_FOUND"))
            call.respond(preview)
        }

        rateLimit(JOIN_GAMES) {
            post("{code}/players") {
                val code = call.gameCode() ?: return@post call.respondUnknownCode()
                val request = call.receive<JoinGameRequest>()
                when (val outcome = entry.join(code, request, call.playerSession(), call.baseUrl())) {
                    is JoinOutcome.Joined -> call.respond(outcome.ticket)
                    is JoinOutcome.Refused ->
                        call.respond(HttpStatusCode.Conflict, ErrorResponse(outcome.error.name))

                    JoinOutcome.NotFound ->
                        call.respond(HttpStatusCode.NotFound, ErrorResponse("GAME_NOT_FOUND"))
                }
            }
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.gameCode(): GameCode? =
    GameCode.ofOrNull(parameters["code"].orEmpty())

private suspend fun io.ktor.server.application.ApplicationCall.respondUnknownCode() =
    respond(HttpStatusCode.NotFound, ErrorResponse("INVALID_GAME_CODE"))
