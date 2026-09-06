package fr.ftnl.cardgame.api.dto

import kotlinx.serialization.Serializable

/** Who the browser is, and what it is allowed to do. */
@Serializable
data class MeView(
    val playerId: String,
    val discordConnected: Boolean,
    val discordUsername: String? = null,
    val discordAvatarUrl: String? = null,
    val twitchConnected: Boolean = false,
    val twitchUsername: String? = null,
    val twitchAvatarUrl: String? = null,
    /** The channel name, which is what the table reads a chat from. */
    val twitchLogin: String? = null,
    val isAdmin: Boolean,
    val discordLoginAvailable: Boolean,
    val twitchLoginAvailable: Boolean = false,
    /**
     * Whether this server has a Twitch extension configured at all. Without one there is
     * no panel for a viewer to cheer from, so the bits way of writing a card is not
     * offered rather than offered and then found to lead nowhere.
     */
    val twitchExtensionAvailable: Boolean = false,
    /**
     * Whether the server still holds a Twitch token for this account, which is what the
     * channel point mode runs on. Granted by the sign in itself, so it is true for anybody
     * signed in — and false for a session opened before the game asked for that right, or
     * after a restart, both of which signing in again fixes.
     */
    val twitchRewardsAuthorized: Boolean = false,
)
