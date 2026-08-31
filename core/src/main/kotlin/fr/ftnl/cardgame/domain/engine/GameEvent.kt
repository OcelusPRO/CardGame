package fr.ftnl.cardgame.domain.engine

import fr.ftnl.cardgame.domain.card.CardId
import fr.ftnl.cardgame.domain.game.Round
import fr.ftnl.cardgame.domain.player.PlayerId

/**
 * What actually happened, so the server can push targeted notifications and feed the
 * usage statistics without re-diffing two game snapshots.
 */
sealed interface GameEvent {

    data class PlayerJoined(val playerId: PlayerId) : GameEvent

    data class PlayerLeft(val playerId: PlayerId) : GameEvent

    data class ConnectionChanged(val playerId: PlayerId, val connected: Boolean) : GameEvent

    data object SettingsUpdated : GameEvent

    data object GameStarted : GameEvent

    data class RoundStarted(val number: Int) : GameEvent

    /**
     * The official punchline cards freshly drawn into players' hands this round, so the
     * statistics can count how often a card actually reaches someone. Custom cards carry
     * no id and never appear here.
     */
    data class HandsRefilled(val punchlineCardIds: List<CardId>) : GameEvent

    data class AnswerSubmitted(val playerId: PlayerId) : GameEvent

    data object SubmissionsClosed : GameEvent

    data class ChoiceMade(val playerId: PlayerId) : GameEvent

    /** Carries the whole scored round, which is what the statistics recorder needs. */
    data class RoundEnded(val round: Round) : GameEvent

    data class GameEnded(val winners: List<PlayerId>) : GameEvent

    /** The host reopened the lobby after a finished game, scores wiped for a rematch. */
    data object ReturnedToLobby : GameEvent
}
