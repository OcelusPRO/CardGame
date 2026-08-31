package fr.ftnl.cardgame.support

import fr.ftnl.cardgame.ApplicationServices
import fr.ftnl.cardgame.catalog.CardPack
import fr.ftnl.cardgame.catalog.CatalogPunchline
import fr.ftnl.cardgame.catalog.CatalogSituation
import fr.ftnl.cardgame.domain.card.CardId
import fr.ftnl.cardgame.domain.card.SituationText

/** Puts enough cards in the database for a real game to start. */
suspend fun ApplicationServices.seedTestDeck(situations: Int = 5, punchlines: Int = 60) {
    packRepository.save(CardPack("test", "Pack de test"))
    repeat(situations) { index ->
        situationRepository.save(
            CatalogSituation(CardId("s${index + 1}"), "test", SituationText("Le pire, c'est ____."))
        )
    }
    repeat(punchlines) { index ->
        punchlineRepository.save(
            CatalogPunchline(CardId("p${index + 1}"), "test", "punchline ${index + 1}")
        )
    }
}
