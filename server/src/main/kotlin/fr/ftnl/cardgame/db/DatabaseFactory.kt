package fr.ftnl.cardgame.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import fr.ftnl.cardgame.config.DatabaseConfig
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import javax.sql.DataSource

/**
 * Opens the pooled connection and brings the schema up to date.
 *
 * The schema is owned by the SQL migrations under `db/migration`, never by the Kotlin
 * table objects: tests run the very same files, so a migration that would break in
 * production breaks the build first.
 */
object DatabaseFactory {

    fun connect(config: DatabaseConfig): Database = connect(dataSource(config))

    fun connect(dataSource: DataSource): Database {
        migrate(dataSource)
        return Database.connect(dataSource)
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
