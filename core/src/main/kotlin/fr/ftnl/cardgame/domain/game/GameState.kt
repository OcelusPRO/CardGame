package fr.ftnl.cardgame.domain.game

import fr.ftnl.cardgame.domain.card.PunchlineCard
import fr.ftnl.cardgame.domain.card.SituationCard
import fr.ftnl.cardgame.domain.deck.DrawPile
import fr.ftnl.cardgame.domain.player.Player
import fr.ftnl.cardgame.domain.player.PlayerId
import kotlinx.serialization.Serializable

/**
 * The complete snapshot of a game. It is immutable: every command produces a new
 * instance, which is what gets stored in Redis and broadcast to the players.
 */
@Serializable
data class GameState(
    val code: GameCode,
    val hostId: PlayerId,
    val players: List<Player> = emptyList(),
    val settings: GameSettings = GameSettings(),
    val phase: GamePhase = GamePhase.LOBBY,
    val situations: DrawPile<SituationCard> = DrawPile(emptyList()),
    val punchlines: DrawPile<PunchlineCard> = DrawPile(emptyList()),
    val hands: Map<PlayerId, List<PunchlineCard>> = emptyMap(),
    val scoreboard: Scoreboard = Scoreboard(),
    val round: Round? = null,
    val phaseDeadlineMillis: Long? = null,
    val createdAtMillis: Long = 0,
) {
    fun playerOf(playerId: PlayerId): Player? = players.firstOrNull { it.id == playerId }

    fun contains(playerId: PlayerId): Boolean = playerOf(playerId) != null

    fun isHost(playerId: PlayerId): Boolean = hostId == playerId

    fun handOf(playerId: PlayerId): List<PunchlineCard> = hands[playerId].orEmpty()

    /** Players still online; a disconnected player keeps their score and their seat. */
    val connectedPlayers: List<Player> get() = players.filter { it.connected }

    /** True when the rotating czar also submits an answer, see [GameSettings.czarAnswers]. */
    val czarAnswers: Boolean
        get() = settings.selectionMode == SelectionMode.CZAR && settings.czarAnswers

    /** Players expected to answer this round, which excludes the card czar unless [czarAnswers]. */
    val answeringPlayers: List<Player>
        get() = if (czarAnswers) connectedPlayers
        else connectedPlayers.filter { it.id != round?.czarId }

    /** How many answers each player must provide, driven by the situation holes. */
    val expectedAnswers: Int get() = round?.situation?.blankCount ?: 1

    val isOver: Boolean get() = phase == GamePhase.FINISHED

    /**
     * True while a match is actually being played. Outside of it a dropped player is
     * removed from the table straight away; during it their seat is kept so they can
     * reconnect, and it is only freed once the game ends.
     */
    val isMidGame: Boolean
        get() = phase == GamePhase.SUBMITTING ||
            phase == GamePhase.SELECTING ||
            phase == GamePhase.ROUND_RESULT
}
