package ru.meowmeow.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.meowmeow.notes.data.BackupData
import ru.meowmeow.notes.data.BackupJson
import ru.meowmeow.notes.data.ChecklistItemEntity
import ru.meowmeow.notes.data.OrderEntity
import ru.meowmeow.notes.data.PartEntity
import ru.meowmeow.notes.data.PhotoEntity
import ru.meowmeow.notes.domain.OrderStatus

/**
 * Формат архива: что приложение пишет, должно читаться обратно без потерь,
 * а чужие и испорченные файлы — не приниматься. Снимков в архиве нет,
 * поэтому и Android здесь не нужен: тест целиком на JVM.
 */
class BackupJsonTest {

    private val data = BackupData(
        orders = listOf(
            OrderEntity(
                id = 7,
                model = "LG WD-10480",
                clientName = "Анна Петрова",
                clientContact = "+7 912 345-67-89",
                problem = "Не отжимает, гудит при отжиме",
                status = OrderStatus.PART_ORDERED.key,
                notes = "Ждём насос",
                createdAt = 1_700_000_000_000,
                updatedAt = 1_700_000_500_000,
            ),
        ),
        checklist = listOf(ChecklistItemEntity(1, 7, "Питание есть", true, 0)),
        parts = listOf(PartEntity(1, 7, "Насос", 2, true, 0)),
        photos = listOf(
            PhotoEntity(1, 7, "order_7_be6f34c3.jpg", "до разборки", 1_700_000_000_000, true),
        ),
    )

    @Test
    fun roundTripKeepsEveryTable() {
        val restored = BackupJson.decode(BackupJson.encode(data))

        assertEquals(data, restored)
        assertEquals(OrderStatus.PART_ORDERED.key, restored!!.orders.single().status)
        assertTrue(restored.photos.single().isPinned)
    }

    @Test
    fun archiveCarriesSignatureAndVersion() {
        val text = BackupJson.encode(data, exportedAt = 42)

        assertTrue(text.contains("\"app\": \"meow-meow-zapisi\""))
        assertTrue(text.contains("\"version\": 1"))
        assertTrue(text.contains("\"exportedAt\": 42"))
    }

    @Test
    fun statusKeyPassesThroughVerbatim() {
        val byKey = decodeOrders("""{"id":1,"status":"in_progress"}""")
        assertEquals(OrderStatus.IN_PROGRESS.key, byKey.single().status)

        // Раздел тоже переносится: вкладка определяется на экране, а не в архиве.
        val admin = decodeOrders("""{"id":2,"section":"admin","status":"task"}""")
        assertEquals("admin", admin.single().section)
        assertEquals("task", admin.single().status)

        // Неизвестный ключ из старой версии не роняет импорт: экран покажет
        // статус по умолчанию, но строка сохранится как есть.
        val unknown = decodeOrders("""{"id":3,"status":"архив"}""")
        assertEquals("архив", unknown.single().status)
    }

    @Test
    fun foreignOrBrokenJsonIsRejected() {
        assertNull(BackupJson.decode("""{"hello":"world"}"""))
        assertNull(BackupJson.decode("не json"))
        assertNull(BackupJson.decode("[]"))
        assertNull(BackupJson.decode(""))
    }

    @Test
    fun photoFileNameLosesDirectoriesAndBlankIsDropped() {
        val text = """{"app":"meow-meow-zapisi","photos":[
            {"id":1,"orderId":1,"fileName":"photos/order_1_a.jpg"},
            {"id":2,"orderId":1,"fileName":"..\\..\\databases\\evil.db"},
            {"id":3,"orderId":1,"fileName":"   "}
        ]}"""

        val photos = BackupJson.decode(text)!!.photos

        assertEquals(listOf("order_1_a.jpg", "evil.db"), photos.map { it.fileName })
    }

    @Test
    fun quantityIsClampedLikeInRepository() {
        val text = """{"app":"meow-meow-zapisi","parts":[
            {"id":1,"orderId":1,"name":"Насос","quantity":999}
        ]}"""

        assertEquals(99, BackupJson.decode(text)!!.parts.single().quantity)
    }

    private fun decodeOrders(order: String): List<OrderEntity> =
        BackupJson.decode("""{"app":"meow-meow-zapisi","orders":[$order]}""")!!.orders
}
