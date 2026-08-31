package fr.ftnl.cardgame.api.view

import fr.ftnl.cardgame.api.dto.PlayerView
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.player.Player

/** Projects a player the way every other player at the table may see them. */
class PlayerViewFactory {

    fun create(state: GameState, player: Player): PlayerView = PlayerView(
        id = player.id.value,
        nickname = player.nickname.value,
        avatar = AvatarMapper.toView(player.avatar),
        connected = player.connected,
        score = state.scoreboard.pointsOf(player.id),
        isHost = state.isHost(player.id),
        isCzar = state.round?.czarId == player.id,
        hasAnswered = state.round?.hasSubmitted(player.id) == true,
        hasVoted = state.round?.hasVoted(player.id) == true,
    )
}
