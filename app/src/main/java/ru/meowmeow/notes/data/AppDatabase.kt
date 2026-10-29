package ru.meowmeow.notes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ru.meowmeow.notes.domain.Section

@Database(
    entities = [
        OrderEntity::class,
        ChecklistItemEntity::class,
        PartEntity::class,
        PhotoEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao

    companion object {
        /**
         * Версия 2: у фото появилось закрепление. Колонка с DEFAULT 0,
         * поэтому старые базы обновляются без потери данных.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE photos ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        /**
         * Версия 3: колонка section разграничивает вкладки «Заказы / Админ /
         * Брифы / Маркетинг». Все существующие записи остаются заказами.
         * Статус и раньше хранился строкой-ключом, так что колонка не трогается.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE orders ADD COLUMN section TEXT NOT NULL DEFAULT '${Section.ORDERS.key}'",
                )
            }
        }

        /** Одна база на приложение: SQLite лежит в приватном каталоге, интернет не нужен. */
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "meow-meow-notes.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                // Внешние ключи нужны, чтобы фото и пункты уходили вместе с заказом.
                .build()
    }
}
