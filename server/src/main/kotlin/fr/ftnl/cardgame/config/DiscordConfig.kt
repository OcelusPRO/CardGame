package fr.ftnl.cardgame.config

/** Discord sign in is optional: with no client id configured the button simply disappears. */
data class DiscordConfig(
    val clientId: String,
    val clientSecret: String,
    val redirectUrl: String,
) {
    val enabled: Boolean get() = clientId.isNotBlank() && clientSecret.isNotBlank()
}
