package fr.ftnl.cardgame.config

/** The administration area is open to these Discord accounts, and to nobody else. */
data class AdminConfig(val discordIds: Set<String>) {
    fun grants(discordId: String): Boolean = discordId in discordIds
}
