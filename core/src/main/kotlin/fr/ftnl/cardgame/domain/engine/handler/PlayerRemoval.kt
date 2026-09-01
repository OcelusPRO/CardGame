package fr.ftnl.cardgame.domain.engine.handler

import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.game.Scoreboard
import fr.ftnl.cardgame.domain.player.PlayerId

/**
 * Frees the seats of [playerIds]: drops them from the roster, their hand and their score,
 * and passes the crown to someone still seated when the host is among those removed.
 */
internal fun GameState.withoutPlayers(playerIds: Set<PlayerId>): GameState {
    if (playerIds.isEmpty()) return this
    val remaining = players.filterNot { it.id in playerIds }
    val nextHost = remaining.firstOrNull { it.connected }?.id ?: remaining.firstOrNull()?.id
    return copy(
        players = remaining,
        hostId = if (hostId in playerIds) nextHost ?: hostId else hostId,
        hands = hands - playerIds,
        scoreboard = Scoreboard(scoreboard.points - playerIds),
    )
}
