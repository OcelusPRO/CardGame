package fr.ftnl.cardgame.support

import fr.ftnl.cardgame.catalog.CardPack
import fr.ftnl.cardgame.catalog.CardPackRepository
import fr.ftnl.cardgame.catalog.CatalogPunchline
import fr.ftnl.cardgame.catalog.CatalogSituation
import fr.ftnl.cardgame.catalog.PunchlineCardRepository
import fr.ftnl.cardgame.catalog.SituationCardRepository
import fr.ftnl.cardgame.domain.card.CardId

/** In-memory pack repository, so catalogue logic can be tested without a database. */
class FakeCardPackRepository(packs: List<CardPack> = emptyList()) : CardPackRepository {
    private val storage = packs.associateBy { it.id }.toMutableMap()

    override suspend fun all() = storage.values.toList()
    override suspend fun enabled() = storage.values.filter { it.enabled }
    override suspend fun save(pack: CardPack) {
        storage[pack.id] = pack
    }

    override suspend fun delete(id: String) = storage.remove(id) != null
}

/** In-memory situation repository. */
class FakeSituationCardRepository(cards: List<CatalogSituation> = emptyList()) : SituationCardRepository {
    private val storage = cards.associateBy { it.id }.toMutableMap()

    override suspend fun all() = storage.values.toList()
    override suspend fun enabledIn(packIds: Set<String>) =
        storage.values.filter { it.enabled && it.packId in packIds }

    override suspend fun find(id: CardId) = storage[id]
    override suspend fun save(card: CatalogSituation) {
        storage[card.id] = card
    }

    override suspend fun delete(id: CardId) = storage.remove(id) != null
    override suspend fun count() = storage.size.toLong()
}

/** In-memory punchline repository. */
class FakePunchlineCardRepository(cards: List<CatalogPunchline> = emptyList()) : PunchlineCardRepository {
    private val storage = cards.associateBy { it.id }.toMutableMap()

    override suspend fun all() = storage.values.toList()
    override suspend fun enabledIn(packIds: Set<String>) =
        storage.values.filter { it.enabled && it.packId in packIds }

    override suspend fun find(id: CardId) = storage[id]
    override suspend fun save(card: CatalogPunchline) {
        storage[card.id] = card
    }

    override suspend fun delete(id: CardId) = storage.remove(id) != null
    override suspend fun count() = storage.size.toLong()
}
