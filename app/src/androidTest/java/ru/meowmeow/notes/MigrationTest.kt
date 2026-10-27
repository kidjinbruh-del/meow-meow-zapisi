package ru.meowmeow.notes

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.meowmeow.notes.data.AppDatabase

/**
 * Обновление установленного приложения не должно терять заказы и фото:
 * проверяем переход схемы 1 → 2 на реальном SQLite.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate1To2AddsPinnedColumnAndKeepsPhotos() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                """
                INSERT INTO orders (id, model, clientName, clientContact, problem, status, notes, createdAt, updatedAt)
                VALUES (1, 'LG WD-10480', 'Анна', '', 'Не отжимает', 'in_progress', '', 100, 100)
                """.trimIndent(),
            )
            execSQL(
                "INSERT INTO photos (id, orderId, fileName, caption, createdAt) " +
                    "VALUES (1, 1, 'order_1_test.jpg', '', 200)",
            )
            close()
        }

        val database = helper.runMigrationsAndValidate(TEST_DB, 2, true, AppDatabase.MIGRATION_1_2)
        database.query("SELECT isPinned FROM photos WHERE id = 1").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        database.query("SELECT model FROM orders WHERE id = 1").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals("LG WD-10480", cursor.getString(0))
        }
        database.close()
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
