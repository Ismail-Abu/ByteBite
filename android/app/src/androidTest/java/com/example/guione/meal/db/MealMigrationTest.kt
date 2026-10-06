package com.example.guione.meal.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Replays the shipped Room schemas with [MigrationTestHelper], which reads the
 * exported JSON from androidTest assets. With a single version this proves the
 * v1 schema is createable and carries the expected tables; it is the harness
 * every future migration test plugs into — each schema bump adds a
 * createDatabase(old) / runMigrationsAndValidate(new, MIGRATION) case here, so
 * the brief's "migrations from every shipped schema version, no destructive
 * reset" is enforceable rather than aspirational.
 */
@RunWith(AndroidJUnit4::class)
class MealMigrationTest {

    private val testDb = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MealDatabase::class.java,
    )

    @Test fun createsVersion1FromExportedSchema() {
        helper.createDatabase(testDb, 1).use { db ->
            val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table'")
            val tables = buildList {
                while (cursor.moveToNext()) add(cursor.getString(0))
            }
            cursor.close()
            assertTrue("meals table missing: $tables", tables.contains("meals"))
            assertTrue("meal_revisions table missing: $tables", tables.contains("meal_revisions"))
        }
    }
}
