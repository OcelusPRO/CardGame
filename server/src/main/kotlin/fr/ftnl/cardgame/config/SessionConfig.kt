package fr.ftnl.cardgame.config

/** Secret used to sign the browser cookie carrying the player and admin session. */
data class SessionConfig(val signKey: String)
