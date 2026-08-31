package fr.ftnl.cardgame.config

/**
 * Loads the bundled French demo deck at boot. Kept off by default so a production
 * database is never polluted with test content.
 */
data class SeedConfig(val enabled: Boolean)
