package fr.ftnl.cardgame.api

import fr.ftnl.cardgame.auth.AdultAccessGuard
import fr.ftnl.cardgame.auth.playerSession
import fr.ftnl.cardgame.catalog.CatalogService
import fr.ftnl.cardgame.domain.game.AnswerMode
import fr.ftnl.cardgame.domain.game.GameCode
import fr.ftnl.cardgame.game.GameDecks
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

/** The public half of the catalogue: which packs a host may switch on. */
fun Route.cardRoutes(catalog: CatalogService, adultAccess: AdultAccessGuard, appliedDecks: GameDecks) {
    get("/api/packs") {
        val answerMode = call.request.queryParameters["answerMode"]
            ?.let { raw -> AnswerMode.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } }

        // With ?code=XXXX the caller is a guest looking at a lobby: show the paquet the
        // host actually built, not the guest's own catalogue. This is what keeps an 18+
        // pack the host has no access to from ever appearing on a guest's screen.
        val applied = call.request.queryParameters["code"]
            ?.let { GameCode.ofOrNull(it) }
            ?.let { appliedDecks.of(it) }
        if (applied != null) {
            call.respond(catalog.packsInGame(applied.packIds, answerMode))
            return@get
        }

        val includeAdult = adultAccess.allows(call.playerSession())
        call.respond(catalog.availablePacks(answerMode, includeAdult))
    }
}
