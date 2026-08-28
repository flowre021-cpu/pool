package com.example.pool.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderRecurrenceTest {
    @Test
    fun encodeDecode_daily() {
        val recurrence = ReminderRecurrence(mode = ReminderRecurrenceMode.DAILY)
        assertEquals("DAILY", recurrence.encode())
        assertEquals(
            ReminderRecurrenceMode.DAILY,
            parseReminderRecurrence("DAILY").mode,
        )
        assertEquals(
            ReminderRecurrenceMode.DAILY,
            parseReminderRecurrence("FREQ=DAILY").mode,
        )
    }

    @Test
    fun encodeDecode_weeklyMultipleDays() {
        val recurrence = ReminderRecurrence(
            mode = ReminderRecurrenceMode.WEEKLY,
            weeklyDays = setOf(1, 3, 5),
        )
        assertEquals("WEEKLY:1,3,5", recurrence.encode())
        val parsed = parseReminderRecurrence("WEEKLY:1,3,5")
        assertEquals(ReminderRecurrenceMode.WEEKLY, parsed.mode)
        assertEquals(setOf(1, 3, 5), parsed.weeklyDays)
    }

    @Test
    fun formatReminderRecurrence_weeklySingleDay() {
        assertEquals(
            "每周周三",
            formatReminderRecurrence(
                ReminderRecurrence(
                    mode = ReminderRecurrenceMode.WEEKLY,
                    weeklyDays = setOf(3),
                ),
            ),
        )
    }

    @Test
    fun formatReminderSummary_recurringWithTimeOnlySemantics() {
        assertEquals(
            "每天",
            formatReminderSummary(null, ReminderRecurrence(mode = ReminderRecurrenceMode.DAILY)),
        )
    }

    @Test
    fun validation_weeklyRequiresDays() {
        assertEquals(
            "请选择重复的星期",
            reminderRecurrenceValidationError(
                ReminderRecurrence(mode = ReminderRecurrenceMode.WEEKLY),
            ),
        )
        assertNull(
            reminderRecurrenceValidationError(
                ReminderRecurrence(mode = ReminderRecurrenceMode.WEEKLY, weeklyDays = setOf(3)),
            ),
        )
    }
}
