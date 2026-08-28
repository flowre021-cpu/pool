package com.example.pool.util

import com.example.pool.data.AffairEntity
import com.example.pool.data.AffairType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReminderOccurrenceCompletionTest {
    private val today = LocalDate.of(2026, 8, 9)
    private val recurringReminder = AffairEntity(
        id = 1L,
        type = AffairType.REMINDER,
        title = "每日提醒",
        recurrenceRule = "DAILY",
    )

    @Test
    fun toggleAffairDone_recurringReminderCompletesOnlyToday() {
        val completed = toggleAffairDone(recurringReminder, today)
        assertFalse(completed.isDone)
        assertTrue(isReminderOccurrenceCompleted(completed, today))
        assertFalse(isReminderOccurrenceCompleted(completed, today.plusDays(1)))
    }

    @Test
    fun toggleAffairDone_recurringReminderCanUndoTodayCompletion() {
        val once = toggleAffairDone(recurringReminder, today)
        val undone = toggleAffairDone(once, today)
        assertFalse(isReminderOccurrenceCompleted(undone, today))
    }

    @Test
    fun toggleAffairDone_nonRecurringStillUsesGlobalIsDone() {
        val task = AffairEntity(
            id = 2L,
            type = AffairType.TASK,
            title = "作业",
        )
        val done = toggleAffairDone(task, today)
        assertTrue(done.isDone)
    }

    @Test
    fun encodeDecode_completedOccurrenceDays() {
        val days = setOf(100L, 101L, 102L)
        assertEquals(days, parseCompletedOccurrenceDays(encodeCompletedOccurrenceDays(days)))
    }
}
