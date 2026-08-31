package fr.ftnl.cardgame.api.view

import fr.ftnl.cardgame.api.dto.DeckSummary
import fr.ftnl.cardgame.api.dto.GameView
import fr.ftnl.cardgame.domain.game.GameClock
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.player.PlayerId

/**
 * Builds the single payload the browser renders. Every "who may see what" decision
 * lives here and in the factories it delegates to.
 */
class GameViewFactory(
    private val clock: GameClock,
    private val players: PlayerViewFactory = PlayerViewFactory(),
    private val rounds: RoundViewFactory = RoundViewFactory(),
    private val self: SelfViewFactory = SelfViewFactory(),
) {

    fun create(state: GameState, viewer: PlayerId): GameView = GameView(
        code = state.code.value,
        phase = state.phase.name,
        hostId = state.hostId.value,
        settings = SettingsMapper.toView(state.settings),
        players = state.players.map { players.create(state, it) },
        you = self.create(state, viewer),
        round = rounds.create(state, viewer),
        deck = DeckSummary(state.situations.size, state.punchlines.size),
        deadlineMillis = state.phaseDeadlineMillis,
        serverTimeMillis = clock.nowMillis(),
    )
}
