package fr.ftnl.cardgame.domain.engine

import fr.ftnl.cardgame.domain.card.CardId
import fr.ftnl.cardgame.domain.card.CardOrigin
import fr.ftnl.cardgame.domain.card.PunchlineCard
import fr.ftnl.cardgame.domain.deck.Shuffler
import fr.ftnl.cardgame.domain.game.GameClock
import fr.ftnl.cardgame.domain.game.GamePhase
import fr.ftnl.cardgame.domain.game.GameState
import fr.ftnl.cardgame.domain.game.Round
import fr.ftnl.cardgame.domain.game.SelectionMode
import fr.ftnl.cardgame.domain.player.PlayerId

/** The next snapshot plus the official cards this round dealt into hands. */
internal data class RoundStart(
    val state: GameState,
    val dealtPunchlines: List<CardId> = emptyList(),
)

/** Opens a round: draws the situation, refills the hands and appoints the czar. */
internal class RoundStarter(
    private val shuffler: Shuffler,
    private val clock: GameClock,
) {

    fun start(state: GameState, number: Int): RoundStart {
        val drawn = state.situations.draw(1, shuffler)
        val situation = drawn.cards.firstOrNull()
            ?: return RoundStart(state.copy(phase = GamePhase.FINISHED, phaseDeadlineMillis = null))
        val dealt = dealHands(state.copy(situations = drawn.pile))
        return RoundStart(
            dealt.state.copy(
                phase = GamePhase.SUBMITTING,
                round = Round(number = number, situation = situation, czarId = czarFor(state, number)),
                phaseDeadlineMillis = clock.nowMillis() + state.settings.submitSeconds * MILLIS_PER_SECOND,
            ),
            dealt.dealtPunchlines,
        )
    }

    private fun czarFor(state: GameState, number: Int): PlayerId? {
        if (state.settings.selectionMode != SelectionMode.CZAR) return null
        val players = state.connectedPlayers.ifEmpty { return null }
        return players[(number - 1) % players.size].id
    }

    private fun dealHands(state: GameState): RoundStart {
        if (!state.settings.dealsCards) return RoundStart(state.copy(hands = emptyMap()))
        var pile = state.punchlines
        val refilled = mutableMapOf<PlayerId, List<PunchlineCard>>()
        val dealt = mutableListOf<CardId>()
        state.connectedPlayers.forEach { player ->
            val hand = state.handOf(player.id)
            val draw = pile.draw(state.settings.handSize - hand.size, shuffler)
            pile = draw.pile
            refilled[player.id] = hand + draw.cards
            draw.cards.forEach { card -> if (card.origin == CardOrigin.OFFICIAL) dealt += card.id }
        }
        return RoundStart(state.copy(punchlines = pile, hands = state.hands + refilled), dealt)
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1000L
    }
}
