package fr.ftnl.cardgame.api.view

import fr.ftnl.cardgame.api.dto.PunchlineCardView
import fr.ftnl.cardgame.api.dto.SelfView
import fr.ftnl.cardgame.domain.card.CardOrigin
import fr.ftnl.cardgame.domain.engine.RoundProgress
import fr.ftnl.cardgame.domain.game.GamePhase
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.player.PlayerId

/** Projects the private half of the view: the viewer hand and what is expected of them. */
class SelfViewFactory {

    fun create(state: GameState, viewer: PlayerId): SelfView = SelfView(
        id = viewer.value,
        hand = state.handOf(viewer).map { card ->
            PunchlineCardView(card.id.value, card.text, card.origin == CardOrigin.CUSTOM, card.blankCount)
        },
        isHost = state.isHost(viewer),
        isCzar = state.round?.czarId == viewer,
        mustAnswer = mustAnswer(state, viewer),
        mustVote = mustVote(state, viewer),
    )

    private fun mustAnswer(state: GameState, viewer: PlayerId): Boolean =
        state.phase == GamePhase.SUBMITTING &&
            state.answeringPlayers.any { it.id == viewer } &&
            state.round?.hasSubmitted(viewer) == false

    private fun mustVote(state: GameState, viewer: PlayerId): Boolean =
        state.phase == GamePhase.SELECTING &&
            viewer in RoundProgress.voters(state) &&
            state.round?.hasVoted(viewer) == false
}
