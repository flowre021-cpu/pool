package com.example.pool.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeekSelectionTest {
    @Test
    fun syncSelectedWeeks_extendsAndAppendsNewWeeks() {
        val result = syncSelectedWeeksForSemesterLength(
            selectedWeeks = (1..20).toList(),
            previousTotal = 20,
            newTotal = 22,
        )
        assertEquals((1..22).toList(), result)
    }

    @Test
    fun syncSelectedWeeks_extendsPartialSelectionWithNewWeeksOnly() {
        val result = syncSelectedWeeksForSemesterLength(
            selectedWeeks = listOf(1, 3, 5),
            previousTotal = 20,
            newTotal = 22,
        )
        assertEquals(listOf(1, 3, 5, 21, 22), result)
    }

    @Test
    fun syncSelectedWeeks_shrinksDropsOutOfRange() {
        val result = syncSelectedWeeksForSemesterLength(
            selectedWeeks = (1..22).toList(),
            previousTotal = 22,
            newTotal = 18,
        )
        assertEquals((1..18).toList(), result)
    }

    @Test
    fun isContiguousFullWeekSelection_detectsFullPrefix() {
        assertTrue(isContiguousFullWeekSelection((1..20).toList()))
        assertFalse(isContiguousFullWeekSelection(listOf(1, 3, 5)))
    }

    @Test
    fun formatSelectedWeeksSummary_usesTotalWeeks() {
        assertEquals("第 1-22 周", formatSelectedWeeksSummary((1..22).toList(), totalWeeks = 22))
        assertEquals("免修：第 21、22 周", formatSelectedWeeksSummary((1..20).toList(), totalWeeks = 22))
    }
}
