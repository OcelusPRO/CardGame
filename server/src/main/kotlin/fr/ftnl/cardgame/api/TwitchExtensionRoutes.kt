package fr.ftnl.cardgame.api

import fr.ftnl.cardgame.api.dto.ErrorResponse
import fr.ftnl.cardgame.api.dto.ExtensionCardRequest
import fr.ftnl.cardgame.api.dto.ExtensionCardResponse
import fr.ftnl.cardgame.config.TwitchExtensionConfig
import fr.ftnl.cardgame.twitch.ExtensionCardService
import fr.ftnl.cardgame.twitch.ExtensionOutcome
import fr.ftnl.cardgame.twitch.ExtensionTokens
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

/**
 * What the Twitch extension talks to.
 *
 * The panel under a stream and the overlay on top of it are the same page, running in a
 * viewer's browser on Twitch's domain — so every call arrives with the viewer token Twitch
 * signed, and nothing here trusts a single field that token did not carry. In particular
 * the channel is read from the token, never from the request: a viewer cannot aim a
 * proposal at somebody else's table by editing a form.
 *
 * CORS is answered explicitly because these calls genuinely do come from another origin,
 * and from that origin alone.
 */
fun Route.twitchExtensionRoutes(
    config: TwitchExtensionConfig,
    tokens: ExtensionTokens,
    cards: ExtensionCardService,
) {
    route("/api/twitch/extension") {

        /** What the panel should draw: is a table open, and what does a card cost here? */
        get("state") {
            val viewer = tokens.viewer(call.token())
                ?: return@get call.respond(HttpStatusCode.Unauthorized, ErrorResponse("BAD_TOKEN"))
            call.respond(cards.state(viewer.channelId, config))
        }

        /** A viewer sends a card, having paid for it with bits when the host asks for that. */
        post("cards") {
            val viewer = tokens.viewer(call.token())
                ?: return@post call.respond(HttpStatusCode.Unauthorized, ErrorResponse("BAD_TOKEN"))
            val request = call.receive<ExtensionCardRequest>()
            when (val outcome = cards.submit(viewer, request, config, tokens)) {
                is ExtensionOutcome.Accepted ->
                    call.respond(ExtensionCardResponse(accepted = true, message = outcome.message))

                is ExtensionOutcome.Refused ->
                    call.respond(outcome.status, ErrorResponse(outcome.code, outcome.message))
            }
        }
    }
}

/** The extension helper sends the viewer token as a bearer, like any other API would. */
private fun ApplicationCall.token(): String? = request.headers[HttpHeaders.Authorization]
