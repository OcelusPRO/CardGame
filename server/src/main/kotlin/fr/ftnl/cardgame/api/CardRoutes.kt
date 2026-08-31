package fr.ftnl.cardgame.api

import fr.ftnl.cardgame.catalog.CatalogService
import fr.ftnl.cardgame.domain.game.AnswerMode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

/** The public half of the catalogue: which packs a host may switch on. */
fun Route.cardRoutes(catalog: CatalogService) {
    get("/api/packs") {
        val answerMode = call.request.queryParameters["answerMode"]
            ?.let { raw -> AnswerMode.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } }
        call.respond(catalog.availablePacks(answerMode))
    }
}
