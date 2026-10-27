package ru.meowmeow.notes.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ru.meowmeow.notes.domain.OrderStatus

/**
 * JSON-архив заказов.
 *
 * Поля названы как колонки таблиц, статус лежит строкой по ключу — как он и
 * хранится в SQLite. Файл можно открыть глазами и поправить блокнотом, а
 * переименование enum в коде не сломает старые архивы.
 *
 * Снимков в архиве нет: это мегабайты файлов, а не строки. В архиве только
 * имя файла, при импорте берутся те снимки, что уже лежат в `files/photos`.
 */
object BackupJson {

    /** Метка в файле: посторонний JSON не должен притворяться архивом. */
    const val APP = "meow-meow-zapisi"

    /** Версия формата. Читаем и более новые файлы: лишние поля игнорируются. */
    const val VERSION = 1

    private val format = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun encode(data: BackupData, exportedAt: Long = System.currentTimeMillis()): String =
        format.encodeToString(
            ArchiveDto.serializer(),
            ArchiveDto(
                app = APP,
                exportedAt = exportedAt,
                orders = data.orders.map { it.toDto() },
                checklist = data.checklist.map { it.toDto() },
                parts = data.parts.map { it.toDto() },
                photos = data.photos.map { it.toDto() },
            ),
        )

    /** null — это не JSON или не наш архив; текст ошибки мастеру ничего не даст. */
    fun decode(text: String): BackupData? {
        val archive = runCatching {
            format.decodeFromString(ArchiveDto.serializer(), text)
        }.getOrNull() ?: return null
        if (archive.app != APP) return null
        return BackupData(
            orders = archive.orders.map { it.toEntity() },
            checklist = archive.checklist.map { it.toEntity() },
            parts = archive.parts.map { it.toEntity() },
            photos = archive.photos.mapNotNull { it.toEntity() },
        )
    }

    /**
     * Только имя файла, без каталогов: архив не должен уметь ссылаться на
     * чужие пути, а перенос базы между устройствами не меняет имя снимка.
     */
    private fun String.bareName(): String = replace('\\', '/').substringAfterLast('/').trim()

    /** Ключ из базы, но принимаем и имя константы — файл правят руками. */
    private fun statusOf(raw: String): OrderStatus =
        OrderStatus.entries.firstOrNull { it.key == raw }
            ?: OrderStatus.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
            ?: OrderStatus.DIAGNOSIS

    private fun OrderEntity.toDto() = OrderDto(
        id = id,
        model = model,
        clientName = clientName,
        clientContact = clientContact,
        problem = problem,
        status = status.key,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun OrderDto.toEntity() = OrderEntity(
        id = id,
        model = model,
        clientName = clientName,
        clientContact = clientContact,
        problem = problem,
        status = statusOf(status),
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun ChecklistItemEntity.toDto() = ChecklistDto(
        id = id,
        orderId = orderId,
        label = label,
        isChecked = isChecked,
        position = position,
    )

    private fun ChecklistDto.toEntity() = ChecklistItemEntity(
        id = id,
        orderId = orderId,
        label = label,
        isChecked = isChecked,
        position = position,
    )

    private fun PartEntity.toDto() = PartDto(
        id = id,
        orderId = orderId,
        name = name,
        quantity = quantity,
        isBought = isBought,
        position = position,
    )

    private fun PartDto.toEntity() = PartEntity(
        id = id,
        orderId = orderId,
        name = name,
        quantity = quantity.coerceIn(1, 99),
        isBought = isBought,
        position = position,
    )

    private fun PhotoEntity.toDto() = PhotoDto(
        id = id,
        orderId = orderId,
        fileName = fileName.bareName(),
        caption = caption,
        createdAt = createdAt,
        isPinned = isPinned,
    )

    private fun PhotoDto.toEntity(): PhotoEntity? {
        val name = fileName.bareName()
        if (name.isEmpty()) return null
        return PhotoEntity(
            id = id,
            orderId = orderId,
            fileName = name,
            caption = caption,
            createdAt = createdAt,
            isPinned = isPinned,
        )
    }
}

@Serializable
private data class ArchiveDto(
    // Пустая метка значит «файл не наш»: значение по умолчанию здесь нельзя,
    // иначе любой чужой JSON без поля app читался бы как пустой архив.
    val app: String = "",
    val version: Int = BackupJson.VERSION,
    val exportedAt: Long = 0,
    val orders: List<OrderDto> = emptyList(),
    val checklist: List<ChecklistDto> = emptyList(),
    val parts: List<PartDto> = emptyList(),
    val photos: List<PhotoDto> = emptyList(),
)

@Serializable
private data class OrderDto(
    val id: Long = 0,
    val model: String = "",
    val clientName: String = "",
    val clientContact: String = "",
    val problem: String = "",
    val status: String = OrderStatus.DIAGNOSIS.key,
    val notes: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

@Serializable
private data class ChecklistDto(
    val id: Long = 0,
    val orderId: Long = 0,
    val label: String = "",
    val isChecked: Boolean = false,
    val position: Int = 0,
)

@Serializable
private data class PartDto(
    val id: Long = 0,
    val orderId: Long = 0,
    val name: String = "",
    val quantity: Int = 1,
    val isBought: Boolean = false,
    val position: Int = 0,
)

@Serializable
private data class PhotoDto(
    val id: Long = 0,
    val orderId: Long = 0,
    val fileName: String = "",
    val caption: String = "",
    val createdAt: Long = 0,
    val isPinned: Boolean = false,
)
