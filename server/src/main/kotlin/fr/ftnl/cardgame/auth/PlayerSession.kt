package fr.ftnl.cardgame.auth

import kotlinx.serialization.Serializable

/**
 * The identity carried by the browser cookie. It survives a refresh, which is what lets
 * a player come back to their seat, and it is the only thing the WebSocket trusts.
 *
 * Both sign ins are optional and independent: a player may carry one, the other, or both.
 * [twitchLogin] is the channel name, and the only field the chat reader ever needs.
 */
@Serializable
data class PlayerSession(
    val playerId: String,
    val discordId: String? = null,
    val discordUsername: String? = null,
    val discordAvatarUrl: String? = null,
    val twitchId: String? = null,
    val twitchLogin: String? = null,
    val twitchUsername: String? = null,
    val twitchAvatarUrl: String? = null,
    /** When the Twitch account was opened, read once at sign in for the 18+ age rule. */
    val twitchCreatedAtMillis: Long? = null,
) {
    /** The signed in accounts behind this browser, in the order they were linked. */
    fun accounts(): List<Account> = listOfNotNull(
        discordId?.let {
            Account(
                provider = AccountProvider.DISCORD,
                id = it,
                displayName = discordUsername.orEmpty(),
                avatarUrl = discordAvatarUrl,
                createdAtMillis = DiscordSnowflake.createdAtMillis(it),
            )
        },
        twitchId?.let {
            Account(
                provider = AccountProvider.TWITCH,
                id = it,
                displayName = twitchUsername.orEmpty(),
                // The channel name travels along: an administrator may be listed by it.
                login = twitchLogin,
                avatarUrl = twitchAvatarUrl,
                createdAtMillis = twitchCreatedAtMillis,
            )
        },
    )
}
