package fr.ftnl.cardgame.api.view

import fr.ftnl.cardgame.api.dto.GameSettingsInput
import fr.ftnl.cardgame.api.dto.GameSettingsView
import fr.ftnl.cardgame.domain.game.AnswerMode
import fr.ftnl.cardgame.domain.game.GameSettings
import fr.ftnl.cardgame.domain.game.ScoringSettings
import fr.ftnl.cardgame.domain.game.SelectionMode

/** Translates game settings between the browser payload and the domain value. */
object SettingsMapper {

    /** Applies the fields the host actually sent on top of the current settings. */
    fun merge(current: GameSettings, input: GameSettingsInput): GameSettings = current.copy(
        selectionMode = input.selectionMode?.let { enumOf<SelectionMode>(it) } ?: current.selectionMode,
        answerMode = input.answerMode?.let { enumOf<AnswerMode>(it) } ?: current.answerMode,
        scoring = mergeScoring(current.scoring, input),
        rounds = input.rounds ?: current.rounds,
        handSize = input.handSize ?: current.handSize,
        submitSeconds = input.submitSeconds ?: current.submitSeconds,
        selectSeconds = input.selectSeconds ?: current.selectSeconds,
        resultSeconds = input.resultSeconds ?: current.resultSeconds,
        minPlayers = input.minPlayers ?: current.minPlayers,
        maxPlayers = input.maxPlayers ?: current.maxPlayers,
        allowSelfVote = input.allowSelfVote ?: current.allowSelfVote,
        czarAnswers = input.czarAnswers ?: current.czarAnswers,
        twitchChatVote = input.twitchChatVote ?: current.twitchChatVote,
        twitchGuestChats = input.twitchGuestChats ?: current.twitchGuestChats,
    )

    fun toView(settings: GameSettings): GameSettingsView = GameSettingsView(
        selectionMode = settings.selectionMode.name,
        answerMode = settings.answerMode.name,
        rounds = settings.rounds,
        handSize = settings.handSize,
        submitSeconds = settings.submitSeconds,
        selectSeconds = settings.selectSeconds,
        resultSeconds = settings.resultSeconds,
        minPlayers = settings.minPlayers,
        maxPlayers = settings.maxPlayers,
        allowSelfVote = settings.allowSelfVote,
        czarAnswers = settings.czarAnswers,
        pointsPerVote = settings.scoring.pointsPerVote,
        unanimityBonus = settings.scoring.unanimityBonus,
        twitchChatVote = settings.twitchChatVote,
        twitchGuestChats = settings.twitchGuestChats,
    )

    private fun mergeScoring(current: ScoringSettings, input: GameSettingsInput) = current.copy(
        pointsPerVote = input.pointsPerVote ?: current.pointsPerVote,
        unanimityBonus = input.unanimityBonus ?: current.unanimityBonus,
    )

    private inline fun <reified T : Enum<T>> enumOf(raw: String): T =
        enumValues<T>().firstOrNull { it.name == raw.uppercase() }
            ?: throw IllegalArgumentException("Unknown ${T::class.simpleName}: $raw")
}
