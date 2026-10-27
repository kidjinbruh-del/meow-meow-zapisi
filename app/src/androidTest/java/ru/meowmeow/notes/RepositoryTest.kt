package ru.meowmeow.notes

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ru.meowmeow.notes.data.AppDatabase
import ru.meowmeow.notes.data.OrderRepository
import ru.meowmeow.notes.data.PhotoStore
import java.io.File

/**
 * Проверка на реальном SQLite (in-memory): схема, каскады, запчасти и фото.
 */
@RunWith(AndroidJUnit4::class)
class RepositoryTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: OrderRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = OrderRepository(context, database.orderDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun newOrderComesWithThreeDefaultChecks() = runBlocking {
        val id = repository.createOrder()

        val items = repository.observeChecklist(id).first()
        assertEquals(3, items.size)
        assertEquals(
            listOf("Питание есть", "Шлейф цел", "Плата звонится"),
            items.map { it.label },
        )
        assertEquals(0, items.count { it.isChecked })
    }

    @Test
    fun checklistToggleIsPersisted() = runBlocking {
        val id = repository.createOrder()
        val item = repository.observeChecklist(id).first().first()

        repository.setChecklistChecked(item, true)
        val updated = repository.observeChecklist(id).first().first { it.id == item.id }
        assertTrue(updated.isChecked)

        repository.setChecklistChecked(updated, false)
        assertFalse(repository.observeChecklist(id).first().first { it.id == item.id }.isChecked)
    }

    @Test
    fun customChecklistItemIsAddedAtTheEnd() = runBlocking {
        val id = repository.createOrder()

        repository.addChecklistItem(id, "  Фильтр чистый  ")
        val items = repository.observeChecklist(id).first()

        assertEquals(4, items.size)
        assertEquals("Фильтр чистый", items.last().label)
    }

    @Test
    fun partsSupportQuantityRenameAndBoughtFlag() = runBlocking {
        val id = repository.createOrder()
        repository.addPart(id, "Помпа", quantity = 2)

        var part = repository.observeParts(id).first().single()
        assertEquals(2, part.quantity)
        assertFalse(part.isBought)

        repository.setPartQuantity(part, 5)
        part = repository.observeParts(id).first().single()
        assertEquals(5, part.quantity)

        repository.renamePart(part, "Помпа слива")
        part = repository.observeParts(id).first().single()
        assertEquals("Помпа слива", part.name)

        repository.setPartBought(part, true)
        part = repository.observeParts(id).first().single()
        assertTrue(part.isBought)

        repository.deletePart(part)
        assertTrue(repository.observeParts(id).first().isEmpty())
    }

    @Test
    fun textFieldsAreSavedAndOrderMovesToTop() = runBlocking {
        val first = repository.createOrder()
        Thread.sleep(5)
        val second = repository.createOrder()

        val order = repository.getOrder(first)!!
        repository.save(
            order.copy(
                model = "LG WD-10480",
                clientName = "Анна",
                problem = "Не отжимает",
                notes = "Купил аналог, не подошел",
            ),
        )

        val saved = repository.getOrder(first)!!
        assertEquals("LG WD-10480", saved.model)
        assertEquals("Анна", saved.clientName)
        assertEquals("Купил аналог, не подошел", saved.notes)
        assertTrue(saved.updatedAt >= order.updatedAt)

        val cards = repository.cards.first()
        assertEquals(listOf(first, second), cards.map { it.order.id })
    }

    @Test
    fun photoIsStoredCountingAndThumbnailDecodes() = runBlocking {
        val id = repository.createOrder()
        val file = repository.newPhotoFile(id)
        writeJpeg(file)

        repository.attachPhoto(id, file)

        val photos = repository.observePhotos(id).first()
        assertEquals(1, photos.size)
        assertEquals(file.name, photos.single().fileName)
        assertTrue(file.exists())

        val thumbnail = PhotoStore.thumbnail(context, photos.single().fileName, 160)
        assertNotNull(thumbnail)
        thumbnail?.recycle()

        val cards = repository.cards.first()
        assertEquals(1, cards.single().photosCount)

        // Убираем за собой: удаление фото должно снести и файл.
        repository.deletePhoto(photos.single())
        assertTrue(repository.observePhotos(id).first().isEmpty())
        assertFalse(file.exists())
    }

    @Test
    fun photosCanBePinnedInBatchAndSortFirst() = runBlocking {
        val id = repository.createOrder()
        val first = repository.newPhotoFile(id)
        writeJpeg(first)
        repository.attachPhoto(id, first)
        Thread.sleep(5)
        val second = repository.newPhotoFile(id)
        writeJpeg(second)
        repository.attachPhoto(id, second)

        var photos = repository.observePhotos(id).first()
        assertEquals(listOf(first.name, second.name), photos.map { it.fileName })

        // Закрепляем сразу оба снимка — верх должен стать закреплённым.
        repository.setPhotosPinned(photos, true)
        photos = repository.observePhotos(id).first()
        assertEquals(2, photos.count { it.isPinned })

        repository.setPhotosPinned(photos.filter { it.fileName == first.name }, false)
        photos = repository.observePhotos(id).first()
        assertEquals(second.name, photos.first().fileName)
        assertTrue(photos.first().isPinned)
        assertFalse(photos.first { it.fileName == first.name }.isPinned)

        photos.forEach { repository.deletePhoto(it) }
        assertTrue(repository.observePhotos(id).first().isEmpty())
    }

    @Test
    fun emptyPhotoFileIsIgnored() = runBlocking {
        val id = repository.createOrder()
        val empty = repository.newPhotoFile(id)
        empty.createNewFile()

        repository.attachPhoto(id, empty)

        assertTrue(repository.observePhotos(id).first().isEmpty())
        assertFalse(empty.exists())
    }

    @Test
    fun deletingOrderRemovesChildrenAndPhotoFiles() = runBlocking {
        val id = repository.createOrder()
        repository.addPart(id, "ТЭН")
        val file = repository.newPhotoFile(id)
        writeJpeg(file)
        repository.attachPhoto(id, file)
        repository.addChecklistItem(id, "Помпа крутится")

        repository.deleteOrder(id)

        assertNull(repository.getOrder(id))
        assertTrue(repository.observeChecklist(id).first().isEmpty())
        assertTrue(repository.observeParts(id).first().isEmpty())
        assertTrue(repository.observePhotos(id).first().isEmpty())
        assertFalse(file.exists())
    }

    @Test
    fun archiveRestoresOrderChildrenAndPhoto() = runBlocking {
        val id = repository.createOrder()
        repository.save(repository.getOrder(id)!!.copy(model = "LG WD-10480", clientName = "Анна"))
        repository.addPart(id, "Насос", quantity = 2)
        val file = repository.newPhotoFile(id)
        writeJpeg(file)
        repository.attachPhoto(id, file)

        val json = repository.exportJson()
        assertTrue(json.contains("meow-meow-zapisi"))

        // Мастер успел почистить карточку — импорт должен вернуть данные.
        repository.deletePart(repository.observeParts(id).first().single())
        repository.save(repository.getOrder(id)!!.copy(model = ""))

        val report = repository.importJson(json)

        assertEquals(1, report.orders)
        assertEquals(1, report.parts)
        assertEquals(1, report.photos)
        assertEquals(0, report.photosMissing)
        assertEquals("LG WD-10480", repository.getOrder(id)!!.model)
        assertEquals("Насос", repository.observeParts(id).first().single().name)
        assertEquals(3, repository.observeChecklist(id).first().size)
        assertEquals(file.name, repository.observePhotos(id).first().single().fileName)

        repository.deleteOrder(id)
    }

    @Test
    fun archiveRecreatesOrderButSkipsPhotosWithoutFiles() = runBlocking {
        val id = repository.createOrder()
        val file = repository.newPhotoFile(id)
        writeJpeg(file)
        repository.attachPhoto(id, file)
        val json = repository.exportJson()

        // Заказ удалён целиком, вместе с файлом снимка.
        repository.deleteOrder(id)
        assertFalse(file.exists())

        val report = repository.importJson(json)

        assertEquals(1, report.orders)
        assertEquals(0, report.photos)
        assertEquals(1, report.photosMissing)
        assertNotNull(repository.getOrder(id))
        assertTrue(repository.observePhotos(id).first().isEmpty())

        repository.deleteOrder(id)
    }

    @Test
    fun importRejectsForeignJson() = runBlocking {
        val result = runCatching { repository.importJson("""{"hello":"world"}""") }

        assertTrue(result.isFailure)
    }

    private fun writeJpeg(file: File) {
        file.parentFile?.mkdirs()
        val bitmap = Bitmap.createBitmap(80, 60, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.RED)
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
        }
        bitmap.recycle()
    }
}
