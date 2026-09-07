package fr.ftnl.cardgame.api.view

import fr.ftnl.cardgame.api.dto.ChatCardLogView
import fr.ftnl.cardgame.api.dto.ChatWrittenCardView
import fr.ftnl.cardgame.api.dto.DeckSummary
import fr.ftnl.cardgame.api.dto.GameView
import fr.ftnl.cardgame.api.dto.SelfView
import fr.ftnl.cardgame.domain.game.ChatCardLog
import fr.ftnl.cardgame.domain.game.GameClock
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.player.PlayerId

/**
 * Builds the single payload the browser renders. Every "who may see what" decision
 * lives here and in the factories it delegates to.
 *
 * There are two audiences. [create] projects the table for one of its players, hand
 * included. [spectate] projects it for the stream page, where the hand is precisely what
 * must not appear: the streamer puts that page on screen so their chat can follow the
 * game without reading the cards they are holding.
 */
class GameViewFactory(
    private val clock: GameClock,
    private val players: PlayerViewFactory = PlayerViewFactory(),
    private val rounds: RoundViewFactory = RoundViewFactory(),
    private val self: SelfViewFactory = SelfViewFactory(),
) {

    fun create(state: GameState, viewer: PlayerId): GameView = base(state, viewer).copy(
        you = self.create(state, viewer),
        chatCardLog = if (state.isHost(viewer)) logOf(state.chatCardLog) else ChatCardLogView(),
    )

    /**
     * The same table with nobody's half of it: no hand, no seat, nothing owed to anyone.
     * `you` is still there — the browser renders one shape — but it is empty, and every
     * flag it carries is false, so the screens that offer an action offer none of them.
     */
    fun spectate(state: GameState): GameView = base(state, viewer = null).copy(spectator = true)

    private fun base(state: GameState, viewer: PlayerId?) = GameView(
        code = state.code.value,
        phase = state.phase.name,
        hostId = state.hostId.value,
        settings = SettingsMapper.toView(state.settings),
        players = state.players.map { players.create(state, it) },
        you = SelfView(id = viewer?.value.orEmpty()),
        round = rounds.create(state, viewer),
        deck = DeckSummary(state.situations.size, state.punchlines.size),
        deadlineMillis = state.phaseDeadlineMillis,
        serverTimeMillis = clock.nowMillis(),
        chatChannels = state.chatChannels,
        chatCardLog = ChatCardLogView(),
    )

    private fun logOf(log: ChatCardLog) = ChatCardLogView(
        situations = log.situations.map { ChatWrittenCardView(it.id, it.text) },
        punchlines = log.punchlines.map { ChatWrittenCardView(it.id, it.text) },
    )
}
