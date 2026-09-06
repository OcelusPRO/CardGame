package fr.ftnl.cardgame.api.dto

import kotlinx.serialization.Serializable

/** Settings sent by the host; every field falls back on the domain default when absent. */
@Serializable
data class GameSettingsInput(
    val selectionMode: String? = null,
    val selectionFormat: String? = null,
    val answerMode: String? = null,
    val rounds: Int? = null,
    val handSize: Int? = null,
    val submitSeconds: Int? = null,
    val selectSeconds: Int? = null,
    val duelSeconds: Int? = null,
    val resultSeconds: Int? = null,
    val minPlayers: Int? = null,
    val maxPlayers: Int? = null,
    val allowSelfVote: Boolean? = null,
    val czarAnswers: Boolean? = null,
    val pointsPerVote: Int? = null,
    val unanimityBonus: Int? = null,
    val twitchGuestChats: Boolean? = null,
    val chatCards: ChatCardsInput? = null,
)

/** Chat card rules sent by the host; an absent field keeps whatever the game has. */
@Serializable
data class ChatCardsInput(
    val access: String? = null,
    val situations: Boolean? = null,
    val punchlines: Boolean? = null,
    val minBits: Int? = null,
    val situationReward: ChatCardRewardInput? = null,
    val punchlineReward: ChatCardRewardInput? = null,
)

/**
 * A reward the host is choosing, renaming or repricing; any part may come on its own.
 * An empty [id] switches back to a reward the game creates itself.
 */
@Serializable
data class ChatCardRewardInput(
    val id: String? = null,
    val title: String? = null,
    val cost: Int? = null,
)
