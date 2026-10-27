package ru.meowmeow.notes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ru.meowmeow.notes.domain.OrderStatus

class Converters {
    @TypeConverter
    fun fromStatus(status: OrderStatus): String = status.key

    @TypeConverter
    fun toStatus(key: String?): OrderStatus = OrderStatus.fromKey(key)
}

@Database(
    entities = [
        OrderEntity::class,
        ChecklistItemEntity::class,
        PartEntity::class,
        PhotoEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
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

        /** Одна база на приложение: SQLite лежит в приватном каталоге, интернет не нужен. */
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "meow-meow-notes.db")
                .addMigrations(MIGRATION_1_2)
                // Внешние ключи нужны, чтобы фото и пункты уходили вместе с заказом.
                .build()
    }
}
