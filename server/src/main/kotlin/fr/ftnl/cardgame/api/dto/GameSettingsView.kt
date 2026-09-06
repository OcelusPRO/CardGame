package fr.ftnl.cardgame.api.dto

import kotlinx.serialization.Serializable

/**
 * Game settings on the wire. Enumerations travel as their name so the browser contract
 * never breaks when the domain gains a value.
 */
@Serializable
data class GameSettingsView(
    /** Who judges: `VOTE`, `CZAR` or `CHAT`. */
    val selectionMode: String,
    /** How they judge: `ALL_AT_ONCE` or `DUELS`. Independent of [selectionMode]. */
    val selectionFormat: String = "ALL_AT_ONCE",
    val answerMode: String,
    val rounds: Int,
    val handSize: Int,
    val submitSeconds: Int,
    val selectSeconds: Int,
    /** How long one duel lasts, in the duel mode; ignored everywhere else. */
    val duelSeconds: Int,
    val resultSeconds: Int,
    val minPlayers: Int,
    val maxPlayers: Int,
    val allowSelfVote: Boolean,
    val czarAnswers: Boolean,
    val pointsPerVote: Int,
    val unanimityBonus: Int,
    val twitchGuestChats: Boolean = false,
    val chatCards: ChatCardsView = ChatCardsView(),
)

/** Whether — and at what price — the viewers may write cards into this game. */
@Serializable
data class ChatCardsView(
    /** `OFF`, `EVERYONE`, `CHANNEL_POINTS` or `BITS`. */
    val access: String = "OFF",
    val situations: Boolean = true,
    val punchlines: Boolean = true,
    val minBits: Int = 100,
    /** The rewards the game puts on the channel in the `CHANNEL_POINTS` mode. */
    val situationReward: ChatCardRewardView = ChatCardRewardView(),
    val punchlineReward: ChatCardRewardView = ChatCardRewardView(),
)

/**
 * One channel point reward for a pile. A blank [id] means the game will create it, under
 * the [title] and [cost] given here; a filled one names a reward already on the channel,
 * whose title and cost are then Twitch's copy of them.
 */
@Serializable
data class ChatCardRewardView(val id: String = "", val title: String = "", val cost: Int = 500)
