package fr.ftnl.cardgame.catalog

import fr.ftnl.cardgame.domain.card.CardId
import fr.ftnl.cardgame.domain.card.CardOrigin
import fr.ftnl.cardgame.domain.card.PunchlineCard
import fr.ftnl.cardgame.domain.card.SituationCard
import fr.ftnl.cardgame.domain.card.SituationText
import java.util.UUID

/**
 * Turns the raw texts typed in the deck builder into cards. They get a throwaway id
 * because they only ever live inside one game session.
 */
class CustomCardFactory {

    fun situations(texts: List<String>): List<SituationCard> = texts.clean().map { text ->
        SituationCard(CardId("custom-s-${UUID.randomUUID()}"), SituationText(text), CardOrigin.CUSTOM)
    }

    fun punchlines(texts: List<String>): List<PunchlineCard> = texts.clean().map { text ->
        PunchlineCard(CardId("custom-p-${UUID.randomUUID()}"), text, CardOrigin.CUSTOM)
    }

    private fun List<String>.clean(): List<String> =
        map { it.trim() }.filter { it.isNotEmpty() && it.length <= MAX_LENGTH }.distinct().take(MAX_CARDS)

    private companion object {
        const val MAX_LENGTH = 200
        const val MAX_CARDS = 500
    }
}
