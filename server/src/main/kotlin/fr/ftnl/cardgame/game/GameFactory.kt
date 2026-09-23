package fr.ftnl.cardgame.game

import fr.ftnl.cardgame.domain.game.GameClock
import fr.ftnl.cardgame.domain.game.GameCode
import fr.ftnl.cardgame.domain.game.GameSettings
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.game.Scoreboard
import fr.ftnl.cardgame.domain.player.Player

/** Builds the empty lobby a freshly created game starts from. */
class GameFactory(private val clock: GameClock) {

    /** A [sharedDevice] table belongs to the [host]'s device, every seat added to it included. */
    fun create(code: GameCode, host: Player, settings: GameSettings, sharedDevice: Boolean = false): GameState =
        GameState(
            code = code,
            hostId = host.id,
            players = listOf(host),
            settings = settings,
            scoreboard = Scoreboard().withPlayer(host.id),
            createdAtMillis = clock.nowMillis(),
            deviceOwner = host.id.takeIf { sharedDevice },
        )
}
