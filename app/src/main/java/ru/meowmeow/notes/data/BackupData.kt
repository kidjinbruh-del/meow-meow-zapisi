package ru.meowmeow.notes.data

/** Согласованный снимок всех таблиц для экспорта и восстановления. */
data class BackupData(
    val orders: List<OrderEntity>,
    val checklist: List<ChecklistItemEntity>,
    val parts: List<PartEntity>,
    val photos: List<PhotoEntity>,
)

/**
 * Итог импорта. Считаем по факту применённого, а не по размеру файла:
 * фото без файла в `files/photos` пропускаются, и мастер должен это видеть.
 */
data class ImportReport(
    val orders: Int,
    val checklist: Int,
    val parts: Int,
    val photos: Int,
    val photosMissing: Int,
)
