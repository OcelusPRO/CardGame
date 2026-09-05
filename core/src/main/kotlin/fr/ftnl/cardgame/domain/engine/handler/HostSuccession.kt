package fr.ftnl.cardgame.domain.engine.handler

import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.player.PlayerId

/**
 * Who takes the crown from [current]: the earliest joined player still online, or —
 * failing that — simply the earliest joined player left at the table. Null when
 * [current] is the only seat.
 */
internal fun GameState.successorTo(current: PlayerId): PlayerId? {
    val others = players.filter { it.id != current }
    return others.firstOrNull { it.connected }?.id ?: others.firstOrNull()?.id
}
