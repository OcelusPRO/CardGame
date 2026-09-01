package fr.ftnl.cardgame.support

import fr.ftnl.cardgame.config.DatabaseConfig
import fr.ftnl.cardgame.db.DatabaseFactory
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.postgresql.ds.PGSimpleDataSource
import java.sql.DriverManager
import javax.sql.DataSource

/**
 * The PostgreSQL of `docker compose`, on a database of its own.
 *
 * Tests run the same engine and the same migrations as production, so a piece of SQL that
 * would break in production cannot pass here. Each test starts from an empty catalogue.
 */
object TestDatabase {

    private val TABLES = listOf(
        "card_usage",
        "combo_stats",
        "daily_activity",
        "adult_pack_access",
        "punchline_cards",
        "situation_cards",
        "card_packs",
    )

    private val url = env("TEST_DATABASE_URL", "jdbc:postgresql://localhost:5432/cardgame_test")
    private val user = env("TEST_DATABASE_USER", "cardgame")
    private val password = env("TEST_DATABASE_PASSWORD", "cardgame")

    private val database: Database by lazy {
        runCatching {
            createDatabase(nameOf(url))
            DatabaseFactory.connect(DatabaseConfig(url, user, password, POOL_SIZE, DRIVER))
        }.getOrElse { failure -> throw IllegalStateException(HELP, failure) }
    }

    /** Hands back the test database, emptied of everything a previous test left behind. */
    fun connect(): Database = database.also(::clear)

    /**
     * A brand new, empty database, for the rare test that needs to watch the migrations
     * themselves run. The caller is responsible for dropping it.
     */
    fun createScratchDatabase(name: String): DataSource {
        createDatabase(name)
        return PGSimpleDataSource().apply {
            setUrl(urlFor(name))
            this.user = TestDatabase.user
            this.password = TestDatabase.password
        }
    }

    fun dropDatabase(name: String) = admin { statement ->
        statement.execute("DROP DATABASE IF EXISTS $name WITH (FORCE)")
    }

    /** Runs a statement on the scratch database, used to plant a schema Flyway never made. */
    fun executeOn(name: String, sql: String) =
        DriverManager.getConnection(urlFor(name), user, password).use { connection ->
            connection.createStatement().use { it.execute(sql) }
        }

    private fun clear(database: Database) = transaction(database) {
        exec("TRUNCATE TABLE ${TABLES.joinToString()} RESTART IDENTITY CASCADE")
    }

    /**
     * Creates the database on first run, so a developer only ever has to start the compose
     * stack; no volume reset and no manual SQL.
     */
    private fun createDatabase(name: String) = admin { statement ->
        val exists = statement.executeQuery("SELECT 1 FROM pg_database WHERE datname = '$name'")
            .use { it.next() }
        if (!exists) statement.execute("CREATE DATABASE $name")
    }

    private fun <T> admin(block: (java.sql.Statement) -> T): T =
        DriverManager.getConnection(urlFor("postgres"), user, password).use { connection ->
            connection.createStatement().use(block)
        }

    private fun urlFor(name: String) = "${url.substringBeforeLast('/')}/$name"

    private fun nameOf(jdbcUrl: String) = jdbcUrl.substringAfterLast('/').substringBefore('?')

    private fun env(name: String, fallback: String) = System.getenv(name) ?: fallback

    private const val DRIVER = "org.postgresql.Driver"
    private const val POOL_SIZE = 4
    private const val HELP =
        "PostgreSQL est introuvable pour les tests. Lancez `docker compose up -d postgres`, " +
            "ou pointez TEST_DATABASE_URL / TEST_DATABASE_USER / TEST_DATABASE_PASSWORD ailleurs."
}
