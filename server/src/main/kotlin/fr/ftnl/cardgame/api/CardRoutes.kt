package fr.ftnl.cardgame.api

import fr.ftnl.cardgame.auth.AdultAccessGuard
import fr.ftnl.cardgame.auth.playerSession
import fr.ftnl.cardgame.catalog.CatalogService
import fr.ftnl.cardgame.domain.game.AnswerMode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

/** The public half of the catalogue: which packs a host may switch on. */
fun Route.cardRoutes(catalog: CatalogService, adultAccess: AdultAccessGuard) {
    get("/api/packs") {
        val answerMode = call.request.queryParameters["answerMode"]
            ?.let { raw -> AnswerMode.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } }
        val includeAdult = adultAccess.allows(call.playerSession().discordId)
        call.respond(catalog.availablePacks(answerMode, includeAdult))
    }
}
