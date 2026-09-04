package fr.ftnl.cardgame.api.dto

import kotlinx.serialization.Serializable

/** Settings sent by the host; every field falls back on the domain default when absent. */
@Serializable
data class GameSettingsInput(
    val selectionMode: String? = null,
    val answerMode: String? = null,
    val rounds: Int? = null,
    val handSize: Int? = null,
    val submitSeconds: Int? = null,
    val selectSeconds: Int? = null,
    val resultSeconds: Int? = null,
    val minPlayers: Int? = null,
    val maxPlayers: Int? = null,
    val allowSelfVote: Boolean? = null,
    val czarAnswers: Boolean? = null,
    val pointsPerVote: Int? = null,
    val unanimityBonus: Int? = null,
    val twitchGuestChats: Boolean? = null,
)
