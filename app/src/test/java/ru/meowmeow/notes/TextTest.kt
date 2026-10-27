package ru.meowmeow.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.meowmeow.notes.domain.OrderStatus
import ru.meowmeow.notes.domain.checkedLabel
import ru.meowmeow.notes.domain.formatWhen
import ru.meowmeow.notes.domain.partsLabel
import ru.meowmeow.notes.domain.photosLabel
import ru.meowmeow.notes.domain.problemPreview
import java.util.Calendar

class TextTest {

    @Test
    fun partsUseRussianPlurals() {
        assertEquals("1 деталь", partsLabel(1))
        assertEquals("3 детали", partsLabel(3))
        assertEquals("5 деталей", partsLabel(5))
        assertEquals("11 деталей", partsLabel(11))
        assertEquals("21 деталь", partsLabel(21))
        assertEquals("0 деталей", partsLabel(0))
    }

    @Test
    fun photosKeepOneForm() {
        assertEquals("0 фото", photosLabel(0))
        assertEquals("1 фото", photosLabel(1))
        assertEquals("7 фото", photosLabel(7))
    }

    @Test
    fun checkedCounterShowsProgress() {
        assertEquals("2 из 4", checkedLabel(2, 4))
        assertEquals("0 из 3", checkedLabel(0, 3))
    }

    @Test
    fun statusLabelsAndFallback() {
        assertEquals("Ждет запчасть", OrderStatus.PART_ORDERED.short)
        assertEquals("Заказана запчасть", OrderStatus.PART_ORDERED.label)
        assertEquals(OrderStatus.DIAGNOSIS, OrderStatus.fromKey("unknown"))
        assertEquals(OrderStatus.DONE, OrderStatus.fromKey("done"))
        assertEquals(OrderStatus.REFUSED, OrderStatus.fromKey("refused"))
        assertEquals("Отказ", OrderStatus.REFUSED.short)
        assertTrue(OrderStatus.IN_PROGRESS.isActive)
        assertFalse(OrderStatus.DONE.isActive)
        assertFalse(OrderStatus.REFUSED.isActive)
        assertTrue(OrderStatus.DONE.isFinished)
        assertTrue(OrderStatus.REFUSED.isFinished)
        assertFalse(OrderStatus.IN_PROGRESS.isFinished)
    }

    @Test
    fun dateIsTodayYesterdayOrDate() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 26, 15, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val todayNoon = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val yesterday = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, -1)
        }.timeInMillis
        val lastYear = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.YEAR, -1)
        }.timeInMillis

        assertEquals("сегодня, 12:00", formatWhen(todayNoon, now))
        assertEquals("вчера", formatWhen(yesterday, now))
        assertTrue(formatWhen(lastYear, now).contains("25"))
    }

    @Test
    fun problemPreviewTakesFirstLineAndCutsLongText() {
        assertEquals("", problemPreview(""))
        assertEquals("", problemPreview("   \n  "))
        assertEquals("Не отжимает", problemPreview("Не отжимает\nГудит"))
        val long = "а".repeat(200)
        val preview = problemPreview(long, limit = 20)
        assertEquals(20, preview.length)
        assertTrue(preview.endsWith("…"))
    }
}
