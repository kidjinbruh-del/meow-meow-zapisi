package ru.meowmeow.notes.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import ru.meowmeow.notes.domain.Section

/**
 * Заказ — одна единица техники в ремонте. Раздел (orders/admin/briefs/marketing)
 * отличает вкладки главного экрана; текстовые поля не ограничены по длине.
 */
@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val section: String = Section.ORDERS.key,
    val model: String = "",
    val clientName: String = "",
    val clientContact: String = "",
    val problem: String = "",
    val status: String = Section.ORDERS.defaultStatus.key,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

/** Пункт блока «Что проверено»: конкретная проверка и её результат. */
@Entity(
    tableName = "checklist_items",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("orderId")],
)
data class ChecklistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val label: String,
    val isChecked: Boolean = false,
    val position: Int = 0,
)

/** Запчасть: что купить и куплено ли уже. */
@Entity(
    tableName = "parts",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("orderId")],
)
data class PartEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val name: String,
    val quantity: Int = 1,
    val isBought: Boolean = false,
    val position: Int = 0,
)

/** Фото хранится файлом в files/photos, в базе — только имя файла. */
@Entity(
    tableName = "photos",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("orderId")],
)
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val fileName: String,
    val caption: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
)

/** Строка главного экрана: заказ вместе со счётчиками для карточки. */
data class OrderCard(
    @Embedded val order: OrderEntity,
    val partsCount: Int,
    val partsBought: Int,
    val photosCount: Int,
)
