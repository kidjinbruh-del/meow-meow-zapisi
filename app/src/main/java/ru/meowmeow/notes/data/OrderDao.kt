package ru.meowmeow.notes.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {

    @Query(
        """
        SELECT o.*,
            (SELECT COUNT(*) FROM parts p WHERE p.orderId = o.id) AS partsCount,
            (SELECT COUNT(*) FROM parts p WHERE p.orderId = o.id AND p.isBought = 1) AS partsBought,
            (SELECT COUNT(*) FROM photos ph WHERE ph.orderId = o.id) AS photosCount
        FROM orders o
        ORDER BY o.updatedAt DESC
        """
    )
    fun observeCards(): Flow<List<OrderCard>>

    @Query("SELECT * FROM orders WHERE id = :id")
    fun observeOrder(id: Long): Flow<OrderEntity?>

    @Query("SELECT * FROM orders WHERE id = :id")
    suspend fun getOrder(id: Long): OrderEntity?

    @Insert
    suspend fun insertOrder(order: OrderEntity): Long

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Query("UPDATE orders SET updatedAt = :timestamp WHERE id = :id")
    suspend fun touch(id: Long, timestamp: Long)

    @Query("DELETE FROM orders WHERE id = :id")
    suspend fun deleteOrder(id: Long)

    // --- чек-лист ---

    @Query("SELECT * FROM checklist_items WHERE orderId = :orderId ORDER BY position, id")
    fun observeChecklist(orderId: Long): Flow<List<ChecklistItemEntity>>

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM checklist_items WHERE orderId = :orderId")
    suspend fun nextChecklistPosition(orderId: Long): Int

    @Insert
    suspend fun insertChecklist(item: ChecklistItemEntity): Long

    @Update
    suspend fun updateChecklist(item: ChecklistItemEntity)

    @Query("DELETE FROM checklist_items WHERE id = :id")
    suspend fun deleteChecklistItem(id: Long)

    // --- запчасти ---

    @Query("SELECT * FROM parts WHERE orderId = :orderId ORDER BY position, id")
    fun observeParts(orderId: Long): Flow<List<PartEntity>>

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM parts WHERE orderId = :orderId")
    suspend fun nextPartPosition(orderId: Long): Int

    @Insert
    suspend fun insertPart(part: PartEntity): Long

    @Update
    suspend fun updatePart(part: PartEntity)

    @Query("DELETE FROM parts WHERE id = :id")
    suspend fun deletePart(id: Long)

    // --- фото ---

    @Query("SELECT * FROM photos WHERE orderId = :orderId ORDER BY isPinned DESC, createdAt, id")
    fun observePhotos(orderId: Long): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE orderId = :orderId")
    suspend fun photosOf(orderId: Long): List<PhotoEntity>

    @Insert
    suspend fun insertPhoto(photo: PhotoEntity): Long

    @Query("UPDATE photos SET caption = :caption WHERE id = :id")
    suspend fun updatePhotoCaption(id: Long, caption: String)

    @Query("UPDATE photos SET isPinned = :pinned WHERE id IN (:ids)")
    suspend fun setPhotosPinned(ids: List<Long>, pinned: Boolean)

    @Query("DELETE FROM photos WHERE id = :id")
    suspend fun deletePhoto(id: Long)

    // --- архив: снимок и восстановление ---

    @Query("SELECT * FROM orders ORDER BY id")
    suspend fun allOrders(): List<OrderEntity>

    @Query("SELECT * FROM checklist_items ORDER BY id")
    suspend fun allChecklist(): List<ChecklistItemEntity>

    @Query("SELECT * FROM parts ORDER BY id")
    suspend fun allParts(): List<PartEntity>

    @Query("SELECT * FROM photos ORDER BY id")
    suspend fun allPhotos(): List<PhotoEntity>

    @Upsert
    suspend fun upsertOrders(orders: List<OrderEntity>)

    @Upsert
    suspend fun upsertChecklist(items: List<ChecklistItemEntity>)

    @Upsert
    suspend fun upsertParts(parts: List<PartEntity>)

    @Upsert
    suspend fun upsertPhotos(photos: List<PhotoEntity>)

    /**
     * Восстановление одной транзакцией. Сначала заказы — на них ссылаются
     * остальные таблицы, — потом вложенные записи.
     *
     * Именно `@Upsert`, а не `INSERT OR REPLACE`: REPLACE удаляет строку и
     * вставляет заново, а удаление заказа каскадом снесло бы его чек-лист,
     * запчасти и фото, которых нет в старом архиве. Upsert обновляет на месте.
     */
    @Transaction
    suspend fun restore(data: BackupData) {
        upsertOrders(data.orders)
        upsertChecklist(data.checklist)
        upsertParts(data.parts)
        upsertPhotos(data.photos)
    }
}
