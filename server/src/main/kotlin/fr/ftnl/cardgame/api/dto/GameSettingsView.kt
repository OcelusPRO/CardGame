package fr.ftnl.cardgame.api.dto

import kotlinx.serialization.Serializable

/**
 * Game settings on the wire. Enumerations travel as their name so the browser contract
 * never breaks when the domain gains a value.
 */
@Serializable
data class GameSettingsView(
    val selectionMode: String,
    val answerMode: String,
    val rounds: Int,
    val handSize: Int,
    val submitSeconds: Int,
    val selectSeconds: Int,
    val resultSeconds: Int,
    val minPlayers: Int,
    val maxPlayers: Int,
    val allowSelfVote: Boolean,
    val czarAnswers: Boolean,
    val pointsPerVote: Int,
    val unanimityBonus: Int,
    val twitchChatVote: Boolean = false,
    val twitchGuestChats: Boolean = false,
)
