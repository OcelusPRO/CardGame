package fr.ftnl.cardgame.catalog

import fr.ftnl.cardgame.domain.card.CardId
import fr.ftnl.cardgame.domain.card.CardOrigin
import fr.ftnl.cardgame.domain.card.SituationCard
import fr.ftnl.cardgame.domain.card.SituationText

/** A situation card as stored in the database, with the bookkeeping the game does not need. */
data class CatalogSituation(
    val id: CardId,
    val packId: String,
    val text: SituationText,
    val enabled: Boolean = true,
    val createdAtMillis: Long = 0,
) {
    fun toDomain(): SituationCard = SituationCard(id, text, CardOrigin.OFFICIAL)
}
