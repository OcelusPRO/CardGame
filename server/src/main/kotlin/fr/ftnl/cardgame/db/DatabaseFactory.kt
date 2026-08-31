package fr.ftnl.cardgame.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import fr.ftnl.cardgame.config.DatabaseConfig
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.slf4j.LoggerFactory
import javax.sql.DataSource

/**
 * Opens the pooled connection and brings the schema up to date.
 *
 * The schema is owned by the SQL migrations under `db/migration`, never by the Kotlin
 * table objects: tests run the very same files, so a migration that would break in
 * production breaks the build first.
 */
object DatabaseFactory {

    private val logger = LoggerFactory.getLogger(DatabaseFactory::class.java)

    /** How long to keep retrying a database that is not up yet, and the pause between tries. */
    private const val STARTUP_TIMEOUT_MILLIS = 60_000L
    private const val RETRY_DELAY_MILLIS = 3_000L

    fun connect(config: DatabaseConfig): Database {
        val dataSource = dataSource(config)
        return try {
            connect(dataSource)
        } catch (failure: Throwable) {
            (dataSource as? HikariDataSource)?.close()
            throw failure
        }
    }

    fun connect(dataSource: DataSource): Database {
        migrate(dataSource)
        return Database.connect(dataSource)
    }

    /**
     * Same as [connect], but tolerant of a database that is still starting.
     *
     * `docker compose restart` and most "restart" buttons do not wait for `depends_on`
     * health, so on a restart the app races Postgres coming back up. Without this it would
     * hit one refused connection and exit for good; with it, it retries for a minute.
     */
    fun connectAwaitingDatabase(config: DatabaseConfig): Database {
        val deadlineMillis = System.currentTimeMillis() + STARTUP_TIMEOUT_MILLIS
        var attempt = 0
        while (true) {
            attempt++
            try {
                return connect(config)
            } catch (failure: Exception) {
                if (System.currentTimeMillis() >= deadlineMillis) {
                    logger.error("Database still unreachable after ${attempt} attempts, giving up", failure)
                    throw failure
                }
                logger.warn(
                    "Database not ready yet (attempt {}): {} — retrying in {} ms",
                    attempt,
                    failure.message,
                    RETRY_DELAY_MILLIS,
                )
                Thread.sleep(RETRY_DELAY_MILLIS)
            }
        }
    }

    /**
     * Applies every pending migration, in order, exactly once.
     *
     * Baselining is deliberately off. On a database holding tables Flyway did not create,
     * baselining would record them as "already migrated" and skip the real schema, leaving
     * a server that starts happily and then fails on the first query.
     */
    fun migrate(dataSource: DataSource) {
        Flyway.configure()
            .dataSource(dataSource)
            .locations(MIGRATIONS)
            .load()
            .migrate()
    }

    private fun dataSource(config: DatabaseConfig): DataSource = HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = config.url
            username = config.user
            password = config.password
            driverClassName = config.driver
            maximumPoolSize = config.poolSize
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        }
    )

    private const val MIGRATIONS = "classpath:db/migration"
}
