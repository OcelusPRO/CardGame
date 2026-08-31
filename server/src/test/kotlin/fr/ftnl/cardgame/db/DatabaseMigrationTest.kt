package fr.ftnl.cardgame.db

import fr.ftnl.cardgame.catalog.CardPack
import fr.ftnl.cardgame.catalog.CatalogPunchline
import fr.ftnl.cardgame.catalog.ExposedCardPackRepository
import fr.ftnl.cardgame.catalog.ExposedPunchlineCardRepository
import fr.ftnl.cardgame.domain.card.CardId
import fr.ftnl.cardgame.support.TestDatabase
import fr.ftnl.cardgame.db.DatabaseFactory
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * The migrations under `db/migration` are the single source of truth for the schema.
 * These checks run against the same PostgreSQL engine as production.
 */
class DatabaseMigrationTest {

    private val packs = ExposedCardPackRepository()
    private val punchlines = ExposedPunchlineCardRepository()

    @BeforeTest
    fun setUp() {
        TestDatabase.connect()
    }

    @Test
    fun `every migration has been applied`() {
        val applied = transaction {
            exec("SELECT version FROM flyway_schema_history WHERE success = true ORDER BY installed_rank") { rows ->
                generateSequence { if (rows.next()) rows.getString(1) else null }.toList()
            }
        }

        assertTrue(applied.orEmpty().contains("1"), "expected migration V1 to be applied, got $applied")
    }

    @Test
    fun `the schema created by the migrations is the one the code expects`() = runBlocking {
        packs.save(CardPack("base", "Base", "Le pack de départ"))
        punchlines.save(CatalogPunchline(CardId("p1"), "base", "une réponse"))

        assertEquals(1, punchlines.count())
        assertEquals("une réponse", punchlines.find(CardId("p1"))?.text)
    }

    @Test
    fun `a card cannot reference a pack that does not exist`() = runBlocking {
        assertFailsWith<Exception> {
            punchlines.save(CatalogPunchline(CardId("p1"), "fantome", "orpheline"))
        }
        Unit
    }

    /**
     * Regression guard. Baselining used to be on, so a database still holding a
     * pre-Flyway schema was recorded as already migrated: the server started, then failed
     * on the first query against a column the migrations were supposed to create.
     */
    @Test
    fun `a schema Flyway did not create is refused instead of silently accepted`() {
        val name = "cardgame_stale_test"
        TestDatabase.dropDatabase(name)
        val scratch = TestDatabase.createScratchDatabase(name)
        TestDatabase.executeOn(name, "CREATE TABLE card_packs (id VARCHAR(64) PRIMARY KEY)")

        try {
            assertFailsWith<Exception> { DatabaseFactory.migrate(scratch) }
        } finally {
            TestDatabase.dropDatabase(name)
        }
    }

    @Test
    fun `an empty database is migrated from scratch`() {
        val name = "cardgame_fresh_test"
        TestDatabase.dropDatabase(name)
        val scratch = TestDatabase.createScratchDatabase(name)

        try {
            DatabaseFactory.migrate(scratch)
            scratch.connection.use { connection ->
                connection.createStatement().use { statement ->
                    val rows = statement.executeQuery("SELECT activity_day FROM daily_activity")
                    assertEquals(false, rows.next())
                }
            }
        } finally {
            TestDatabase.dropDatabase(name)
        }
    }
}
