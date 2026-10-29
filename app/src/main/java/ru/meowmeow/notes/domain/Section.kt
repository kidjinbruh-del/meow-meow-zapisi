package ru.meowmeow.notes.domain

/**
 * Раздел главного экрана. Вкладки «Заказы / Админ / Брифы / Маркетинг» —
 * это одна база карточек, разграниченная колонкой section.
 */
enum class Section(
    val key: String,
    val label: String,
    val createLabel: String,
    val searchHint: String,
    val titleCardLabel: String,
    val titleLabel: String,
    val problemLabel: String,
    val notesLabel: String,
    val emptyText: String,
    val statuses: List<StatusDef>,
) {
    ORDERS(
        key = "orders",
        label = "Заказы",
        createLabel = "Новый заказ",
        searchHint = "Модель, клиент или симптом",
        titleCardLabel = "Техника и клиент",
        titleLabel = "Модель техники",
        problemLabel = "Проблема со слов клиента",
        notesLabel = "Ход ремонта",
        emptyText = "Нажмите «Новый заказ» — карточка ремонта появится здесь",
        statuses = listOf(
            StatusDef("diagnosis", "Диагностика", "Диагностика", StatusTint.GRAY, active = true),
            StatusDef("in_progress", "В работе", "В работе", StatusTint.AMBER, active = true),
            StatusDef("part_ordered", "Заказана запчасть", "Ждет запчасть", StatusTint.BLUE, active = true),
            StatusDef("done", "Готово", "Готово", StatusTint.GREEN, finished = true),
            StatusDef("refused", "Отказ", "Отказ", StatusTint.RED, finished = true),
        ),
    ),
    ADMIN(
        key = "admin",
        label = "Админ",
        createLabel = "Новая заметка",
        searchHint = "Название, узел или команда",
        titleCardLabel = "Заметка админа",
        titleLabel = "Название",
        problemLabel = "Подробности",
        notesLabel = "Логи и команды",
        emptyText = "Нажмите «Новая заметка» — здесь будут заметки сетевого и системного администратора",
        statuses = listOf(
            StatusDef("note", "Заметка", "Заметка", StatusTint.GRAY, active = true),
            StatusDef("task", "Задача", "Задача", StatusTint.AMBER, active = true),
            StatusDef("in_progress", "В работе", "В работе", StatusTint.BLUE, active = true),
            StatusDef("done", "Готово", "Готово", StatusTint.GREEN, finished = true),
            StatusDef("archive", "Архив", "Архив", StatusTint.RED, finished = true),
        ),
    ),
    BRIEFS(
        key = "briefs",
        label = "Брифы",
        createLabel = "Новый бриф",
        searchHint = "Тема или ключевое слово",
        titleCardLabel = "Бриф с диктофона",
        titleLabel = "Тема брифа",
        problemLabel = "Кратко",
        notesLabel = "Расшифровка записи",
        emptyText = "Нажмите «Новый бриф», надиктуйте текст через микрофон — ИИ переведёт его в заметку",
        statuses = listOf(
            StatusDef("new", "Новый", "Новый", StatusTint.GRAY, active = true),
            StatusDef("recorded", "Записан", "Записан", StatusTint.AMBER, active = true),
            StatusDef("transcribed", "Расшифрован", "Расшифрован", StatusTint.PURPLE, active = true),
            StatusDef("done", "Оформлен", "Оформлен", StatusTint.GREEN, finished = true),
            StatusDef("archive", "Архив", "Архив", StatusTint.RED, finished = true),
        ),
    ),
    MARKETING(
        key = "marketing",
        label = "Маркетинг",
        createLabel = "Новая идея",
        searchHint = "Название идеи или площадки",
        titleCardLabel = "Маркетинг",
        titleLabel = "Название",
        problemLabel = "Суть идеи",
        notesLabel = "Заметки по запуску",
        emptyText = "Нажмите «Новая идея» — планы по продвижению и их результат появятся здесь",
        statuses = listOf(
            StatusDef("idea", "Идея", "Идея", StatusTint.GRAY, active = true),
            StatusDef("in_progress", "В работе", "В работе", StatusTint.AMBER, active = true),
            StatusDef("launched", "Запущено", "Запущено", StatusTint.TEAL, active = true),
            StatusDef("result", "Есть результат", "Результат", StatusTint.GREEN, finished = true),
            StatusDef("archive", "Архив", "Архив", StatusTint.RED, finished = true),
        ),
    ),
    ;

    val defaultStatus: StatusDef get() = statuses.first()

    /** Неизвестный ключ (старая версия, ручная правка архива) не должен ронять экран. */
    fun statusOf(key: String?): StatusDef = statuses.firstOrNull { it.key == key } ?: defaultStatus

    val hasClientFields: Boolean get() = this == ORDERS
    val hasChecklist: Boolean get() = this == ORDERS
    val hasParts: Boolean get() = this == ORDERS

    companion object {
        fun fromKey(key: String?): Section = entries.firstOrNull { it.key == key } ?: ORDERS
    }
}

/**
 * Статус карточки внутри раздела. key пишется в SQLite, label — то, что видит мастер.
 * У каждого раздела свой набор: «каждый статус по своему».
 */
data class StatusDef(
    val key: String,
    val label: String,
    val short: String,
    val tint: StatusTint,
    val active: Boolean = false,
    val finished: Boolean = false,
)

/** Семантический цвет статуса; конкретные значения берутся из палитры темы. */
enum class StatusTint { GRAY, AMBER, BLUE, GREEN, RED, PURPLE, TEAL }

/** У заказов статус хранится строкой-ключом; для совместимости с архивами. */
val OrderStatus.def: StatusDef
    get() = Section.ORDERS.statusOf(key)
