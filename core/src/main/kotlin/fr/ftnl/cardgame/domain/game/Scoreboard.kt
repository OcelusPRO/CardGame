package fr.ftnl.cardgame.domain.game

import fr.ftnl.cardgame.domain.player.PlayerId
import kotlinx.serialization.Serializable

/** Running total of the game, kept separate from the players so it survives a disconnection. */
@Serializable
data class Scoreboard(val points: Map<PlayerId, Int> = emptyMap()) {

    fun pointsOf(playerId: PlayerId): Int = points[playerId] ?: 0

    /** Adds a round outcome on top of the current totals. */
    operator fun plus(delta: Map<PlayerId, Int>): Scoreboard =
        Scoreboard(points + delta.mapValues { (player, gain) -> pointsOf(player) + gain })

    /** Registers a player with a zero score, keeping an existing total untouched. */
    fun withPlayer(playerId: PlayerId): Scoreboard =
        if (points.containsKey(playerId)) this else Scoreboard(points + (playerId to 0))

    /** Every player tied at the highest score, empty when nobody scored yet. */
    val leaders: List<PlayerId>
        get() = points.filterValues { it > 0 }.let { scored ->
            val best = scored.values.maxOrNull() ?: return emptyList()
            scored.filterValues { it == best }.keys.toList()
        }
}
