package com.example.pool.util

import com.example.pool.data.schedule.Semester
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SemesterCalendarTest {
    private val semester = Semester(
        id = "semester",
        name = "测试学期",
        startDateEpochDay = LocalDate.of(2026, 2, 23).toEpochDay(),
        endDateEpochDay = LocalDate.of(2026, 6, 28).toEpochDay(),
        isActive = true,
    )

    @Test
    fun contains_includesBothBoundaries_andExcludesHoliday() {
        assertTrue(SemesterCalendar.contains(semester, LocalDate.of(2026, 2, 23)))
        assertTrue(SemesterCalendar.contains(semester, LocalDate.of(2026, 6, 28)))
        assertFalse(SemesterCalendar.contains(semester, LocalDate.of(2026, 7, 1)))
    }

    @Test
    fun weekCount_matchesConfiguredDateRange() {
        assertEquals(18, SemesterCalendar.weekCount(semester))
    }
}
