package fr.ftnl.cardgame.config

/**
 * Twitch sign in is optional, exactly like Discord: with no client id configured the
 * button disappears and the chat options never show up in a lobby.
 */
data class TwitchConfig(
    val clientId: String,
    val clientSecret: String,
    val redirectUrl: String,
    /** The IRC gateway the chat reader connects to; overridden by the tests. */
    val chatUrl: String = DEFAULT_CHAT_URL,
) {
    val enabled: Boolean get() = clientId.isNotBlank() && clientSecret.isNotBlank()

    companion object {
        const val DEFAULT_CHAT_URL = "wss://irc-ws.chat.twitch.tv:443"
    }
}
