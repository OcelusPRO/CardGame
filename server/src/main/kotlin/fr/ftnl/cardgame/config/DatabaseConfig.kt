package fr.ftnl.cardgame.config

/** Connection settings of the PostgreSQL instance holding the card catalogue and the statistics. */
data class DatabaseConfig(
    val url: String,
    val user: String,
    val password: String,
    val poolSize: Int,
    val driver: String,
)
