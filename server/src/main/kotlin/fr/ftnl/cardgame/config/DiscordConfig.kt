package fr.ftnl.cardgame.config

/**
 * Discord sign in is optional: with no client id configured the button simply disappears.
 *
 * [botToken] is optional too, and unrelated to signing in: Discord only tells who is
 * behind an id to a bot, so without one the administration cannot fill a pseudo in by
 * itself and the label is typed by hand.
 */
data class DiscordConfig(
    val clientId: String,
    val clientSecret: String,
    val redirectUrl: String,
    val botToken: String = "",
) {
    val enabled: Boolean get() = clientId.isNotBlank() && clientSecret.isNotBlank()

    val canLookUpAccounts: Boolean get() = botToken.isNotBlank()
}
