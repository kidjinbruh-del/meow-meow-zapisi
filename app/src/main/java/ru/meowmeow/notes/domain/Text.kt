package ru.meowmeow.notes.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Русские склонения без библиотек. */
object Plural {
    fun of(count: Int, one: String, few: String, many: String): String {
        val abs = if (count < 0) -count else count
        val lastTwo = abs % 100
        if (lastTwo in 11..14) return many
        return when (abs % 10) {
            1 -> one
            2, 3, 4 -> few
            else -> many
        }
    }
}

private val RU = Locale("ru", "RU")
private val TIME = SimpleDateFormat("HH:mm", RU)
private val DAY_MONTH = SimpleDateFormat("d MMM", RU)
private val DAY_MONTH_YEAR = SimpleDateFormat("d MMM yy", RU)

fun partsLabel(count: Int): String = "$count " + Plural.of(count, "деталь", "детали", "деталей")

fun photosLabel(count: Int): String = "$count " + Plural.of(count, "фото", "фото", "фото")

fun ordersLabel(count: Int): String = "$count " + Plural.of(count, "заказ", "заказа", "заказа")

fun notesLabel(count: Int): String = "$count " + Plural.of(count, "заметка", "заметки", "заметок")

fun checkedLabel(done: Int, total: Int): String = "$done из $total"

/** «сегодня, 14:05», «вчера», «12 окт.», «12 окт. 24». */
fun formatWhen(timestamp: Long, now: Long = System.currentTimeMillis()): String {
    val date = Calendar.getInstance().apply { timeInMillis = timestamp }
    val today = Calendar.getInstance().apply { timeInMillis = now }
    val yesterday = Calendar.getInstance().apply {
        timeInMillis = now
        add(Calendar.DAY_OF_YEAR, -1)
    }
    fun sameDay(a: Calendar, b: Calendar) =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

    val moment = Date(timestamp)
    return when {
        sameDay(date, today) -> "сегодня, " + TIME.format(moment)
        sameDay(date, yesterday) -> "вчера"
        date.get(Calendar.YEAR) == today.get(Calendar.YEAR) -> DAY_MONTH.format(moment)
        else -> DAY_MONTH_YEAR.format(moment)
    }
}

/** Короткая строка проблемы для карточки: первая непустая строка, не длиннее лимита. */
fun problemPreview(problem: String, limit: Int = 90): String {
    val line = problem.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() } ?: return ""
    return if (line.length <= limit) line else line.take(limit - 1).trimEnd() + "…"
}
