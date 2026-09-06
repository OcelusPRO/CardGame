package fr.ftnl.cardgame.api.view

import fr.ftnl.cardgame.api.dto.ChatCardsView
import fr.ftnl.cardgame.api.dto.GameSettingsInput
import fr.ftnl.cardgame.api.dto.GameSettingsView
import fr.ftnl.cardgame.domain.game.AnswerMode
import fr.ftnl.cardgame.domain.game.ChatCardAccess
import fr.ftnl.cardgame.domain.game.ChatCardSettings
import fr.ftnl.cardgame.domain.game.GameSettings
import fr.ftnl.cardgame.domain.game.ScoringSettings
import fr.ftnl.cardgame.domain.game.SelectionFormat
import fr.ftnl.cardgame.domain.game.SelectionMode

/** Translates game settings between the browser payload and the domain value. */
object SettingsMapper {

    /** Applies the fields the host actually sent on top of the current settings. */
    fun merge(current: GameSettings, input: GameSettingsInput): GameSettings = current.copy(
        selectionMode = input.selectionMode?.let { enumOf<SelectionMode>(it) } ?: current.selectionMode,
        selectionFormat = input.selectionFormat?.let { enumOf<SelectionFormat>(it) } ?: current.selectionFormat,
        answerMode = input.answerMode?.let { enumOf<AnswerMode>(it) } ?: current.answerMode,
        scoring = mergeScoring(current.scoring, input),
        rounds = input.rounds ?: current.rounds,
        handSize = input.handSize ?: current.handSize,
        submitSeconds = input.submitSeconds ?: current.submitSeconds,
        selectSeconds = input.selectSeconds ?: current.selectSeconds,
        duelSeconds = input.duelSeconds ?: current.duelSeconds,
        resultSeconds = input.resultSeconds ?: current.resultSeconds,
        minPlayers = input.minPlayers ?: current.minPlayers,
        maxPlayers = input.maxPlayers ?: current.maxPlayers,
        allowSelfVote = input.allowSelfVote ?: current.allowSelfVote,
        czarAnswers = input.czarAnswers ?: current.czarAnswers,
        twitchGuestChats = input.twitchGuestChats ?: current.twitchGuestChats,
        chatCards = mergeChatCards(current.chatCards, input),
    )

    fun toView(settings: GameSettings): GameSettingsView = GameSettingsView(
        selectionMode = settings.selectionMode.name,
        selectionFormat = settings.selectionFormat.name,
        answerMode = settings.answerMode.name,
        rounds = settings.rounds,
        handSize = settings.handSize,
        submitSeconds = settings.submitSeconds,
        selectSeconds = settings.selectSeconds,
        duelSeconds = settings.duelSeconds,
        resultSeconds = settings.resultSeconds,
        minPlayers = settings.minPlayers,
        maxPlayers = settings.maxPlayers,
        allowSelfVote = settings.allowSelfVote,
        czarAnswers = settings.czarAnswers,
        pointsPerVote = settings.scoring.pointsPerVote,
        unanimityBonus = settings.scoring.unanimityBonus,
        twitchGuestChats = settings.twitchGuestChats,
        chatCards = ChatCardsView(
            access = settings.chatCards.access.name,
            situations = settings.chatCards.situations,
            punchlines = settings.chatCards.punchlines,
            minBits = settings.chatCards.minBits,
            rewardId = settings.chatCards.rewardId,
        ),
    )

    private fun mergeChatCards(current: ChatCardSettings, input: GameSettingsInput): ChatCardSettings {
        val patch = input.chatCards ?: return current
        return current.copy(
            access = patch.access?.let { enumOf<ChatCardAccess>(it) } ?: current.access,
            situations = patch.situations ?: current.situations,
            punchlines = patch.punchlines ?: current.punchlines,
            minBits = patch.minBits ?: current.minBits,
            rewardId = patch.rewardId?.trim() ?: current.rewardId,
        )
    }

    private fun mergeScoring(current: ScoringSettings, input: GameSettingsInput) = current.copy(
        pointsPerVote = input.pointsPerVote ?: current.pointsPerVote,
        unanimityBonus = input.unanimityBonus ?: current.unanimityBonus,
    )

    private inline fun <reified T : Enum<T>> enumOf(raw: String): T =
        enumValues<T>().firstOrNull { it.name == raw.uppercase() }
            ?: throw IllegalArgumentException("Unknown ${T::class.simpleName}: $raw")
}
