package fr.ftnl.cardgame.api.admin

import fr.ftnl.cardgame.stats.CardKind
import fr.ftnl.cardgame.stats.StatsService
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

/** The numbers behind the administration dashboard. */
fun Route.adminStatsRoutes(stats: StatsService) {
    route("/api/admin/stats") {

        get("overview") {
            call.requireAdmin() ?: return@get
            call.respond(stats.overview())
        }

        get("activity") {
            call.requireAdmin() ?: return@get
            call.respond(stats.activity(call.intParam("days", default = 30, max = 365)))
        }

        get("cards") {
            call.requireAdmin() ?: return@get
            val kind = CardKind.entries.firstOrNull { it.name == call.request.queryParameters["kind"] }
                ?: CardKind.PUNCHLINE
            call.respond(stats.topCards(kind, call.intParam("limit", default = 20, max = 200)))
        }

        get("cards/{id}") {
            call.requireAdmin() ?: return@get
            call.respond(stats.punchlineStats(call.parameters["id"].orEmpty()))
        }

        get("combos") {
            call.requireAdmin() ?: return@get
            call.respond(
                stats.topCombos(
                    limit = call.intParam("limit", default = 20, max = 200),
                    minPlays = call.intParam("minPlays", default = 2, max = 1000),
                )
            )
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.intParam(name: String, default: Int, max: Int): Int =
    request.queryParameters[name]?.toIntOrNull()?.coerceIn(1, max) ?: default
