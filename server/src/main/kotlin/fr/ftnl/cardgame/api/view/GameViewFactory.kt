package fr.ftnl.cardgame.api.view

import fr.ftnl.cardgame.api.dto.ChatCardLogView
import fr.ftnl.cardgame.api.dto.ChatWrittenCardView
import fr.ftnl.cardgame.api.dto.DeckSummary
import fr.ftnl.cardgame.api.dto.GameView
import fr.ftnl.cardgame.api.dto.SelfView
import fr.ftnl.cardgame.domain.engine.RoundProgress
import fr.ftnl.cardgame.domain.game.ChatCardLog
import fr.ftnl.cardgame.domain.game.GameClock
import fr.ftnl.cardgame.domain.game.GamePhase
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
     * What the socket of [device] is shown. Online, that is simply its own seat. A shared
     * device looks through the [seat] it was handed to — or, between two turns, at the
     * [table] everybody around it may read.
     */
    fun forDevice(state: GameState, device: PlayerId, seat: PlayerId?): GameView = when {
        !state.sharedDevice -> create(state, device)
        seat != null && state.contains(seat) -> create(state, seat).copy(seat = seat.value)
        else -> table(state, device)
    }

    /**
     * A shared device facing the room: nobody's hand, no answer marked as anybody's, no
     * vote shown — the phone is on the coffee table, and whoever looks at it must learn
     * nothing the reveal will not tell them. It still acts as the host, since the lobby,
     * the results and the rematch are run from this screen.
     */
    fun table(state: GameState, device: PlayerId): GameView = base(state, viewer = null).copy(
        you = SelfView(id = device.value, isHost = state.isHost(device)),
        chatCardLog = if (state.isHost(device)) logOf(state.chatCardLog) else ChatCardLogView(),
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
        sharedDevice = state.sharedDevice,
        awaiting = if (state.sharedDevice) awaiting(state).map { it.value } else emptyList(),
    )

    /** Who the step on the table still waits for, the order the phone goes round in. */
    private fun awaiting(state: GameState): List<PlayerId> {
        val pending = when (state.phase) {
            GamePhase.SUBMITTING -> RoundProgress.pendingAnswers(state)
            GamePhase.SELECTING -> RoundProgress.pendingVotes(state)
            else -> emptyList()
        }.toSet()
        return state.players.map { it.id }.filter { it in pending }
    }

    private fun logOf(log: ChatCardLog) = ChatCardLogView(
        situations = log.situations.map { ChatWrittenCardView(it.id, it.text) },
        punchlines = log.punchlines.map { ChatWrittenCardView(it.id, it.text) },
    )
}
