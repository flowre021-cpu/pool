package com.example.pool.data.schedule.import

import org.junit.Assert.assertEquals
import org.junit.Test

class BuaaTermSelectionTest {
    @Test
    fun yearOptions_coversRecentYears() {
        val options = BuaaTermSelection.yearOptions(2026)
        assertEquals(listOf(2026, 2025, 2024), options)
    }

    @Test
    fun toTermCode_spring2026() {
        assertEquals(
            "2025-2026-2",
            BuaaTermSelection.toTermCode(2026, BuaaTermSeason.SPRING),
        )
    }

    @Test
    fun toTermCode_summer2026() {
        assertEquals(
            "2025-2026-3",
            BuaaTermSelection.toTermCode(2026, BuaaTermSeason.SUMMER),
        )
    }

    @Test
    fun toTermCode_autumn2026() {
        assertEquals(
            "2026-2027-1",
            BuaaTermSelection.toTermCode(2026, BuaaTermSeason.AUTUMN),
        )
    }

    @Test
    fun displayLabel_formatsChinese() {
        assertEquals(
            "2026 年春季学期",
            BuaaTermSelection.displayLabel(2026, BuaaTermSeason.SPRING),
        )
    }

    @Test
    fun toSemesterId_matchesPoolConvention() {
        assertEquals("2026-spring", BuaaTermSelection.toSemesterId(2026, BuaaTermSeason.SPRING))
        assertEquals("2026-summer", BuaaTermSelection.toSemesterId(2026, BuaaTermSeason.SUMMER))
        assertEquals("2026-fall", BuaaTermSelection.toSemesterId(2026, BuaaTermSeason.AUTUMN))
    }

    @Test
    fun fetchMode_matchesBuaaApi() {
        assertEquals(BuaaScheduleFetchMode.BY_WEEK, BuaaTermSelection.fetchMode(BuaaTermSeason.SPRING))
        assertEquals(BuaaScheduleFetchMode.BY_WEEK, BuaaTermSelection.fetchMode(BuaaTermSeason.SUMMER))
        assertEquals(BuaaScheduleFetchMode.BY_CLASS, BuaaTermSelection.fetchMode(BuaaTermSeason.AUTUMN))
    }
}
