package fr.ftnl.cardgame.game

import fr.ftnl.cardgame.catalog.DeckRequest
import fr.ftnl.cardgame.domain.game.GameCode
import java.util.concurrent.ConcurrentHashMap

/**
 * Remembers the deck a game was last built from, keyed by game code.
 *
 * The game snapshot only carries the flat pile of cards, not where they came from, so
 * this is what lets the server rebuild the pile when the rules change under it — e.g. the
 * host switches the answer mode and a pack that was reserved to the other mode has to go.
 * In memory only: a lost entry just means no automatic pruning until the deck is re-applied.
 */
class GameDecks {

    private val applied = ConcurrentHashMap<String, DeckRequest>()

    fun remember(code: GameCode, request: DeckRequest) {
        applied[code.value] = request
    }

    fun of(code: GameCode): DeckRequest? = applied[code.value]

    fun forget(code: GameCode) {
        applied.remove(code.value)
    }
}
