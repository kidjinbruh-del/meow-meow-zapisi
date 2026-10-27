package ru.meowmeow.notes.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import ru.meowmeow.notes.domain.Defaults
import java.io.File

/**
 * Единственное место, где меняются данные заказа.
 * Фото удаляются вместе с записью в базе — поэтому порядок важен.
 */
class OrderRepository(context: Context, private val dao: OrderDao) {

    private val appContext = context.applicationContext

    val cards: Flow<List<OrderCard>> = dao.observeCards()

    fun observeOrder(id: Long): Flow<OrderEntity?> = dao.observeOrder(id)
    fun observeChecklist(orderId: Long): Flow<List<ChecklistItemEntity>> = dao.observeChecklist(orderId)
    fun observeParts(orderId: Long): Flow<List<PartEntity>> = dao.observeParts(orderId)
    fun observePhotos(orderId: Long): Flow<List<PhotoEntity>> = dao.observePhotos(orderId)

    suspend fun getOrder(id: Long): OrderEntity? = dao.getOrder(id)

    /** Новый заказ сразу с тремя типовыми проверками — мастеру остаётся отмечать. */
    suspend fun createOrder(): Long {
        val id = dao.insertOrder(OrderEntity())
        Defaults.checklist.forEachIndexed { index, label ->
            dao.insertChecklist(ChecklistItemEntity(orderId = id, label = label, position = index))
        }
        return id
    }

    suspend fun save(order: OrderEntity) {
        dao.updateOrder(order.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteOrder(id: Long) {
        dao.photosOf(id).forEach { PhotoStore.delete(appContext, it.fileName) }
        dao.deleteOrder(id)
    }

    // --- чек-лист ---

    suspend fun addChecklistItem(orderId: Long, label: String) {
        val clean = label.trim()
        if (clean.isEmpty()) return
        dao.insertChecklist(
            ChecklistItemEntity(
                orderId = orderId,
                label = clean,
                position = dao.nextChecklistPosition(orderId),
            ),
        )
        touch(orderId)
    }

    suspend fun setChecklistChecked(item: ChecklistItemEntity, checked: Boolean) {
        dao.updateChecklist(item.copy(isChecked = checked))
        touch(item.orderId)
    }

    suspend fun deleteChecklistItem(item: ChecklistItemEntity) {
        dao.deleteChecklistItem(item.id)
        touch(item.orderId)
    }

    // --- запчасти ---

    suspend fun addPart(orderId: Long, name: String, quantity: Int = 1) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        dao.insertPart(
            PartEntity(
                orderId = orderId,
                name = clean,
                quantity = quantity.coerceAtLeast(1),
                position = dao.nextPartPosition(orderId),
            ),
        )
        touch(orderId)
    }

    suspend fun setPartBought(part: PartEntity, bought: Boolean) {
        dao.updatePart(part.copy(isBought = bought))
        touch(part.orderId)
    }

    suspend fun setPartQuantity(part: PartEntity, quantity: Int) {
        dao.updatePart(part.copy(quantity = quantity.coerceIn(1, 99)))
        touch(part.orderId)
    }

    suspend fun renamePart(part: PartEntity, name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        dao.updatePart(part.copy(name = clean))
        touch(part.orderId)
    }

    suspend fun deletePart(part: PartEntity) {
        dao.deletePart(part.id)
        touch(part.orderId)
    }

    // --- фото ---

    fun newPhotoFile(orderId: Long): File = PhotoStore.newFile(appContext, orderId)

    fun photoFile(fileName: String): File = PhotoStore.file(appContext, fileName)

    /** Пустой файл (камера закрылась без снимка) в базу не пишем. */
    suspend fun attachPhoto(orderId: Long, file: File) {
        if (!file.exists() || file.length() == 0L) {
            file.delete()
            return
        }
        dao.insertPhoto(PhotoEntity(orderId = orderId, fileName = file.name))
        touch(orderId)
    }

    suspend fun importPhoto(orderId: Long, uri: android.net.Uri) {
        val file = PhotoStore.copyFromUri(appContext, orderId, uri) ?: return
        attachPhoto(orderId, file)
    }

    suspend fun deletePhoto(photo: PhotoEntity) {
        PhotoStore.delete(appContext, photo.fileName)
        dao.deletePhoto(photo.id)
        touch(photo.orderId)
    }

    /** Закрепить или открепить сразу несколько снимков. */
    suspend fun setPhotosPinned(photos: List<PhotoEntity>, pinned: Boolean) {
        if (photos.isEmpty()) return
        dao.setPhotosPinned(photos.map { it.id }, pinned)
        touch(photos.first().orderId)
    }

    suspend fun setPhotoCaption(photo: PhotoEntity, caption: String) {
        dao.updatePhotoCaption(photo.id, caption.trim())
    }

    // --- архив ---

    /** Согласованный снимок всех таблиц: читаем одной серией запросов. */
    suspend fun snapshot(): BackupData = BackupData(
        orders = dao.allOrders(),
        checklist = dao.allChecklist(),
        parts = dao.allParts(),
        photos = dao.allPhotos(),
    )

    /** Текст архива; экран сам решает, в какой файл его положить. */
    suspend fun exportJson(): String = BackupJson.encode(snapshot())

    /**
     * Импорт архива.
     *
     * Записи с тем же `id` обновляются, новые добавляются, лишние локальные
     * остаются на месте — импорт не может стереть работу, которой не было
     * в архиве. Поэтому же `@Upsert`, а не замена таблиц.
     *
     * Фото подтягиваем только те, чей файл уже лежит в `files/photos`:
     * сам архив снимков не содержит. Вложенные записи без своего заказа
     * отбрасываем — иначе SQLite откажет по внешнему ключу и упадёт весь импорт.
     */
    suspend fun importJson(text: String): ImportReport {
        val archive = BackupJson.decode(text)
            ?: throw IllegalArgumentException("это не архив meow meow записей")

        val orderIds = archive.orders.mapTo(mutableSetOf()) { it.id }
        orderIds += dao.allOrders().map { it.id }

        val photos = archive.photos.filter { photo ->
            photo.orderId in orderIds && PhotoStore.file(appContext, photo.fileName).isFile
        }
        val data = archive.copy(
            checklist = archive.checklist.filter { it.orderId in orderIds },
            parts = archive.parts.filter { it.orderId in orderIds },
            photos = photos,
        )
        dao.restore(data)

        return ImportReport(
            orders = data.orders.size,
            checklist = data.checklist.size,
            parts = data.parts.size,
            photos = data.photos.size,
            photosMissing = archive.photos.size - photos.size,
        )
    }

    private suspend fun touch(orderId: Long) {
        dao.touch(orderId, System.currentTimeMillis())
    }
}
