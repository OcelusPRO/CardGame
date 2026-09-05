package fr.ftnl.cardgame.auth

import fr.ftnl.cardgame.config.DiscordConfig
import fr.ftnl.cardgame.twitch.TwitchAppTokens
import org.slf4j.LoggerFactory

/**
 * Puts a name on what an administrator typed.
 *
 * Nobody remembers an account id, and on Twitch nobody has ever seen one: a channel name
 * is what a streamer is known by. So both are accepted here, and the profile behind them
 * is read from the provider — the pseudo fills itself in, and a channel name is turned
 * into the id that actually gets stored.
 *
 * Every failure is a null: an unconfigured provider, an unknown account, an API having a
 * bad day. The administration then falls back on what was typed.
 */
class AccountDirectory(
    private val discord: DiscordClient,
    private val discordConfig: DiscordConfig,
    private val twitch: TwitchClient,
    private val twitchTokens: TwitchAppTokens,
) : AccountLookup {

    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun find(provider: AccountProvider, query: String): Account? {
        val asked = query.trim().removePrefix("@")
        if (asked.isEmpty()) return null
        return runCatching {
            when (provider) {
                AccountProvider.DISCORD -> discordAccount(asked)
                AccountProvider.TWITCH -> twitchAccount(asked)
            }
        }.onFailure { log.warn("Could not read the {} profile of {}", provider, asked, it) }.getOrNull()
    }

    private suspend fun discordAccount(id: String): Account? {
        if (!discordConfig.canLookUpAccounts || !id.all { it.isDigit() }) return null
        return discord.user(discordConfig.botToken, id)?.account()
    }

    /**
     * An id first when it looks like one, then the channel name — a Twitch login may be
     * all digits, so a number that matches nothing is still worth trying as a name.
     */
    private suspend fun twitchAccount(query: String): Account? {
        val token = twitchTokens.token() ?: return null
        val byId = if (query.all { it.isDigit() }) twitch.viewers(token, listOf(query)) else emptyList()
        val found = byId.firstOrNull() ?: twitch.byLogins(token, listOf(query.lowercase())).firstOrNull()
        return found?.account()
    }
}

/** Raised when an administrator names an account the provider does not know. */
class UnknownAccountException(message: String) : RuntimeException(message)
