package fr.ftnl.cardgame.ws

import fr.ftnl.cardgame.api.dto.DeckInput
import fr.ftnl.cardgame.api.view.SettingsMapper
import fr.ftnl.cardgame.catalog.CardPoolResolver
import fr.ftnl.cardgame.catalog.CustomCardFactory
import fr.ftnl.cardgame.catalog.DeckRequest
import fr.ftnl.cardgame.domain.card.CardId
import fr.ftnl.cardgame.domain.engine.GameCommand
import fr.ftnl.cardgame.domain.game.GameSettings
import fr.ftnl.cardgame.domain.game.SubmissionId
import fr.ftnl.cardgame.domain.player.PlayerId

/**
 * Translates a socket message into the domain command it stands for. Anything needing a
 * database round trip, like resolving a deck, happens here rather than in the engine.
 */
class GameCommandTranslator(
    private val decks: CardPoolResolver,
    private val customCards: CustomCardFactory,
) {

    suspend fun toCommand(
        message: ClientMessage,
        playerId: PlayerId,
        settings: GameSettings,
    ): GameCommand? = when (message) {
        is ClientMessage.PlayCards ->
            GameCommand.PlayCards(playerId, message.cardIds.map(::CardId), message.fills)
        is ClientMessage.WriteAnswers -> GameCommand.WriteAnswers(playerId, message.texts)
        is ClientMessage.Choose -> GameCommand.Choose(playerId, SubmissionId(message.answerId))
        is ClientMessage.UpdateSettings ->
            GameCommand.UpdateSettings(playerId, SettingsMapper.merge(settings, message.settings))

        is ClientMessage.UpdateDeck -> GameCommand.SetCardPool(playerId, resolve(message.deck))
        is ClientMessage.Kick -> GameCommand.Kick(playerId, PlayerId(message.playerId))
        ClientMessage.Start -> GameCommand.Start(playerId)
        ClientMessage.NextRound -> GameCommand.NextRound(playerId)
        ClientMessage.ReturnToLobby -> GameCommand.ReturnToLobby(playerId)
        ClientMessage.Leave -> GameCommand.Leave(playerId)
        ClientMessage.Ping -> null
    }

    private suspend fun resolve(deck: DeckInput) = decks.resolve(
        DeckRequest(
            packIds = deck.packIds,
            customSituations = customCards.situations(deck.customSituations),
            customPunchlines = customCards.punchlines(deck.customPunchlines),
        )
    )
}
