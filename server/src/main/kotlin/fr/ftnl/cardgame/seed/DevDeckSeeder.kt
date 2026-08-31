package fr.ftnl.cardgame.seed

import fr.ftnl.cardgame.catalog.CardPack
import fr.ftnl.cardgame.catalog.CardPackRepository
import fr.ftnl.cardgame.catalog.CatalogPunchline
import fr.ftnl.cardgame.catalog.CatalogSituation
import fr.ftnl.cardgame.catalog.PunchlineCardRepository
import fr.ftnl.cardgame.catalog.SituationCardRepository
import fr.ftnl.cardgame.domain.card.CardId
import fr.ftnl.cardgame.domain.card.SituationText
import fr.ftnl.cardgame.domain.game.GameClock
import kotlinx.serialization.json.Json

/**
 * Fills an empty database with the bundled French demo deck so a local instance is
 * playable immediately. Guarded by `SEED_DEV_DECK`, and it never touches a catalogue
 * that already holds cards.
 */
class DevDeckSeeder(
    private val packs: CardPackRepository,
    private val situations: SituationCardRepository,
    private val punchlines: PunchlineCardRepository,
    private val clock: GameClock,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {

    /** Returns true when the deck was actually inserted. */
    suspend fun seed(): Boolean {
        if (situations.count() > 0 || punchlines.count() > 0) return false
        val deck = load() ?: return false
        packs.save(CardPack(deck.packId, deck.packName, deck.packDescription, true, clock.nowMillis()))
        insertSituations(deck)
        insertPunchlines(deck)
        return true
    }

    private suspend fun insertSituations(deck: DevDeck) {
        deck.situations.forEachIndexed { index, text ->
            situations.save(
                CatalogSituation(
                    id = CardId("${deck.packId}-s${index + 1}"),
                    packId = deck.packId,
                    text = SituationText(text),
                    createdAtMillis = clock.nowMillis(),
                )
            )
        }
    }

    private suspend fun insertPunchlines(deck: DevDeck) {
        deck.punchlines.forEachIndexed { index, text ->
            punchlines.save(
                CatalogPunchline(
                    id = CardId("${deck.packId}-p${index + 1}"),
                    packId = deck.packId,
                    text = text,
                    createdAtMillis = clock.nowMillis(),
                )
            )
        }
    }

    private fun load(): DevDeck? {
        val resource = javaClass.classLoader.getResource(RESOURCE) ?: return null
        return runCatching { json.decodeFromString(DevDeck.serializer(), resource.readText()) }.getOrNull()
    }

    private companion object {
        const val RESOURCE = "dev-deck.json"
    }
}
