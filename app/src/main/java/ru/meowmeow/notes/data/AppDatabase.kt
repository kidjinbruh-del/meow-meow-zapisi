package ru.meowmeow.notes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
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
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao

    companion object {
        /** Одна база на приложение: SQLite лежит в приватном каталоге, интернет не нужен. */
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "meow-meow-notes.db")
                // Внешние ключи нужны, чтобы фото и пункты уходили вместе с заказом.
                .build()
    }
}
