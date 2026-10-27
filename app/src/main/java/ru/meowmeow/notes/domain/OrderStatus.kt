package ru.meowmeow.notes.domain

/**
 * Статус ремонта. key пишется в SQLite, label — то, что видит мастер.
 */
enum class OrderStatus(val key: String, val label: String, val short: String) {
    DIAGNOSIS("diagnosis", "Диагностика", "Диагностика"),
    IN_PROGRESS("in_progress", "В работе", "В работе"),
    PART_ORDERED("part_ordered", "Заказана запчасть", "Ждет запчасть"),
    DONE("done", "Готово", "Готово"),
    REFUSED("refused", "Отказ", "Отказ");

    /** Заказ ещё требует действий мастера. */
    val isActive: Boolean
        get() = this == DIAGNOSIS || this == IN_PROGRESS || this == PART_ORDERED

    /** Заказ закрыт: выдан клиенту или клиент отказался от ремонта. */
    val isFinished: Boolean
        get() = this == DONE || this == REFUSED

    companion object {
        /** Неизвестный ключ (например, из старой версии) не должен ронять экран. */
        fun fromKey(key: String?): OrderStatus = entries.firstOrNull { it.key == key } ?: DIAGNOSIS
    }
}
