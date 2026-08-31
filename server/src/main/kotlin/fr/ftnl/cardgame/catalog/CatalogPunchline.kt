package fr.ftnl.cardgame.catalog

import fr.ftnl.cardgame.domain.card.CardId
import fr.ftnl.cardgame.domain.card.CardOrigin
import fr.ftnl.cardgame.domain.card.PunchlineCard

/** A punchline card as stored in the database. */
data class CatalogPunchline(
    val id: CardId,
    val packId: String,
    val text: String,
    val enabled: Boolean = true,
    val createdAtMillis: Long = 0,
) {
    fun toDomain(): PunchlineCard = PunchlineCard(id, text, CardOrigin.OFFICIAL)
}
