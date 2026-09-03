package fr.ftnl.cardgame.ws

import fr.ftnl.cardgame.api.dto.DeckInput
import fr.ftnl.cardgame.api.view.SettingsMapper
import fr.ftnl.cardgame.catalog.CardPoolResolver
import fr.ftnl.cardgame.catalog.CustomCardFactory
import fr.ftnl.cardgame.catalog.DeckRequest
import fr.ftnl.cardgame.domain.card.CardId
import fr.ftnl.cardgame.domain.engine.GameCommand
import fr.ftnl.cardgame.domain.game.AnswerMode
import fr.ftnl.cardgame.domain.game.GameCode
import fr.ftnl.cardgame.domain.game.GameSettings
import fr.ftnl.cardgame.domain.game.SubmissionId
import fr.ftnl.cardgame.domain.player.PlayerId
import fr.ftnl.cardgame.game.GameDecks

/**
 * Translates a socket message into the domain command it stands for. Anything needing a
 * database round trip, like resolving a deck, happens here rather than in the engine.
 */
class GameCommandTranslator(
    private val decks: CardPoolResolver,
    private val customCards: CustomCardFactory,
    private val applied: GameDecks,
) {

    suspend fun toCommand(
        message: ClientMessage,
        playerId: PlayerId,
        settings: GameSettings,
        code: GameCode,
        allowAdult: Boolean,
    ): GameCommand? = when (message) {
        is ClientMessage.PlayCards ->
            GameCommand.PlayCards(playerId, message.cardIds.map(::CardId), message.fills)
        is ClientMessage.WriteAnswers -> GameCommand.WriteAnswers(playerId, message.texts)
        is ClientMessage.Choose -> GameCommand.Choose(playerId, SubmissionId(message.answerId))
        is ClientMessage.UpdateSettings ->
            GameCommand.UpdateSettings(playerId, SettingsMapper.merge(settings, message.settings))

        is ClientMessage.UpdateDeck -> {
            val request = deckRequest(message.deck)
            applied.remember(code, request)
            GameCommand.SetCardPool(playerId, decks.resolve(request, settings.answerMode, allowAdult))
        }

        is ClientMessage.Kick -> GameCommand.Kick(playerId, PlayerId(message.playerId))
        ClientMessage.Start -> GameCommand.Start(playerId)
        ClientMessage.NextRound -> GameCommand.NextRound(playerId)
        ClientMessage.ReturnToLobby -> GameCommand.ReturnToLobby(playerId)
        ClientMessage.Leave -> GameCommand.Leave(playerId)
        ClientMessage.Ping -> null
    }

    /**
     * Rebuilds the last deck the host applied, for [answerMode]. Null when nothing was
     * applied yet, in which case the game still holds its default (all packs) pool.
     */
    suspend fun poolForMode(
        code: GameCode,
        by: PlayerId,
        answerMode: AnswerMode,
        allowAdult: Boolean,
    ): GameCommand.SetCardPool? {
        val request = applied.of(code) ?: return null
        return GameCommand.SetCardPool(by, decks.resolve(request, answerMode, allowAdult))
    }

    fun forget(code: GameCode) = applied.forget(code)

    /**
     * A line the host typed into "Vos situations" that matches a hidden pack's secret code
     * pulls that pack in and is dropped from the custom cards, so the code never shows up
     * on the table as a situation.
     */
    private suspend fun deckRequest(deck: DeckInput): DeckRequest {
        val unlock = decks.unlock(deck.customSituations)
        return DeckRequest(
            packIds = deck.packIds + unlock.packIds,
            customSituations = customCards.situations(
                deck.customSituations.filter { it.trim() !in unlock.codeLines },
            ),
            customPunchlines = customCards.punchlines(deck.customPunchlines),
        )
    }
}
