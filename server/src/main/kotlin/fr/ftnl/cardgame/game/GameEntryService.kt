package fr.ftnl.cardgame.game

import fr.ftnl.cardgame.api.dto.AvatarInput
import fr.ftnl.cardgame.api.dto.CreateGameRequest
import fr.ftnl.cardgame.api.dto.GamePreview
import fr.ftnl.cardgame.api.dto.GameSettingsInput
import fr.ftnl.cardgame.api.dto.GameTicket
import fr.ftnl.cardgame.api.dto.JoinGameRequest
import fr.ftnl.cardgame.api.view.AvatarMapper
import fr.ftnl.cardgame.api.view.SettingsMapper
import fr.ftnl.cardgame.auth.AdultAccessGuard
import fr.ftnl.cardgame.auth.PlayerSession
import fr.ftnl.cardgame.catalog.CardPoolResolver
import fr.ftnl.cardgame.catalog.DeckRequest
import fr.ftnl.cardgame.domain.engine.GameCommand
import fr.ftnl.cardgame.domain.game.GameCode
import fr.ftnl.cardgame.domain.game.GamePhase
import fr.ftnl.cardgame.domain.game.GameSettings
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.player.Nickname
import fr.ftnl.cardgame.domain.player.Player
import fr.ftnl.cardgame.domain.player.PlayerId

/**
 * Creating and joining a table, which is everything that happens over plain HTTP before
 * the WebSocket takes over.
 */
class GameEntryService(
    private val games: GameService,
    private val decks: CardPoolResolver,
    private val appliedDecks: GameDecks,
    private val adultAccess: AdultAccessGuard,
) {

    suspend fun create(request: CreateGameRequest, session: PlayerSession, baseUrl: String): GameTicket {
        val host = player(session, request.nickname, request.avatar)
        val settings = SettingsMapper.merge(GameSettings(), request.settings ?: GameSettingsInput())
        val state = games.create(host, settings)
        // A brand new table starts on every enabled pack the host is actually allowed to use:
        // adult-only packs are left out unless one of their accounts is cleared for them.
        val allowAdult = adultAccess.allows(session)
        val deck = DeckRequest(packIds = decks.enabledPackIds(includeAdult = allowAdult))
        appliedDecks.remember(state.code, deck)
        games.dispatch(
            state.code,
            GameCommand.SetCardPool(host.id, decks.resolve(deck, settings.answerMode, allowAdult)),
        )
        return ticket(state.code, host.id, baseUrl, isHost = true)
    }

    suspend fun join(
        code: GameCode,
        request: JoinGameRequest,
        session: PlayerSession,
        baseUrl: String,
    ): JoinOutcome {
        val newcomer = player(session, request.nickname, request.avatar)
        return when (val result = games.dispatch(code, GameCommand.Join(newcomer))) {
            DispatchResult.GameNotFound -> JoinOutcome.NotFound
            is DispatchResult.Refused -> JoinOutcome.Refused(result.error)
            is DispatchResult.Updated ->
                JoinOutcome.Joined(ticket(code, newcomer.id, baseUrl, result.state.isHost(newcomer.id)))
        }
    }

    suspend fun preview(code: GameCode, viewer: PlayerId): GamePreview? =
        games.find(code)?.let { state -> toPreview(state, viewer) }

    private fun player(session: PlayerSession, nickname: String, avatar: AvatarInput) = Player(
        id = PlayerId(session.playerId),
        nickname = Nickname.of(nickname),
        avatar = AvatarMapper.toDomain(avatar, session.discordAvatarUrl ?: session.twitchAvatarUrl),
        twitchLogin = session.twitchLogin,
    )

    private fun ticket(code: GameCode, playerId: PlayerId, baseUrl: String, isHost: Boolean) = GameTicket(
        code = code.value,
        playerId = playerId.value,
        joinUrl = "$baseUrl/game/${code.value}",
        isHost = isHost,
    )

    private fun toPreview(state: GameState, viewer: PlayerId) = GamePreview(
        code = state.code.value,
        phase = state.phase.name,
        hostNickname = state.playerOf(state.hostId)?.nickname?.value.orEmpty(),
        playerCount = state.players.size,
        maxPlayers = state.settings.maxPlayers,
        canJoin = state.phase == GamePhase.LOBBY && state.players.size < state.settings.maxPlayers,
        youArePlaying = state.contains(viewer),
    )
}
