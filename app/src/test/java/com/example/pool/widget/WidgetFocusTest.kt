package com.example.pool.widget

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class WidgetFocusTest {
    private val sampleItems = listOf(
        WidgetScheduleItem(1, WidgetScheduleKind.COURSE, "早课", null, 8 * 60, 9 * 60 + 45, 0xFFAAAAAAL),
        WidgetScheduleItem(2, WidgetScheduleKind.COURSE, "数学课", "A101", 14 * 60, 15 * 60 + 35, 0xFFBBBBBBL),
        WidgetScheduleItem(3, WidgetScheduleKind.EVENT, "组会", null, 17 * 60, 17 * 60 + 55, 0xFFCCCCCL),
    )

    @Test
    fun reorderTodayItemsForFocus_startsFromCurrentItemForward() {
        val reordered = WidgetFocus.reorderTodayItemsForFocus(
            items = sampleItems,
            now = LocalTime.of(14, 30),
        )

        assertEquals(listOf("数学课", "组会", "早课"), reordered.map { it.title })
    }

    @Test
    fun reorderTodayItemsForFocus_keepsChronologicalOrderWhenAllPast() {
        val reordered = WidgetFocus.reorderTodayItemsForFocus(
            items = sampleItems,
            now = LocalTime.of(23, 0),
        )

        assertEquals(listOf("早课", "数学课", "组会"), reordered.map { it.title })
    }

    @Test
    fun reorderDeadlineItemsForFocus_preservesDeadlineOrder() {
        val items = listOf(
            WidgetDeadlineItem(1, "最近", "2h", deadlineMillis = 100L),
            WidgetDeadlineItem(2, "次之", "1日", deadlineMillis = 200L),
        )

        assertEquals(items, WidgetFocus.reorderDeadlineItemsForFocus(items))
    }

    @Test
    fun reorderReminderItemsForFocus_startsFromNextUpcoming() {
        val items = listOf(
            WidgetReminderItem(1, "早", startMinutes = 8 * 60),
            WidgetReminderItem(2, "午", startMinutes = 12 * 60),
            WidgetReminderItem(3, "晚", startMinutes = 20 * 60),
        )

        val reordered = WidgetFocus.reorderReminderItemsForFocus(
            items = items,
            now = LocalTime.of(13, 0),
        )

        assertEquals(listOf("晚", "早", "午"), reordered.map { it.title })
    }
}
