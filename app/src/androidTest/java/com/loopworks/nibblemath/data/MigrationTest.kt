package com.loopworks.nibblemath.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.loopworks.nibblemath.data.db.NibbleMathDatabase
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Instrumented schema check. Verifies the v1 database exposes the expected
 * tables. Future migrations are exercised here with Room's MigrationTestHelper
 * against the exported schemas in app/schemas.
 */
class MigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun v1ExposesExpectedTables() {
        val database = Room.inMemoryDatabaseBuilder(context, NibbleMathDatabase::class.java).build()
        val tables = database.openHelper.writableDatabase
            .query(
                "SELECT name FROM sqlite_master " +
                    "WHERE type='table' " +
                    "AND name NOT LIKE 'sqlite_%' " +
                    "AND name NOT IN ('android_metadata', 'room_master_table')",
            )
            .use { cursor ->
                val names = mutableListOf<String>()
                while (cursor.moveToNext()) names += cursor.getString(0)
                names
            }
        assertEquals(
            setOf(
                "books", "recipes", "steps", "ingredients",
                "products", "recipe_ingredients", "pantry_entries", "price_cache",
            ),
            tables.toSet(),
        )
        database.close()
    }
}
