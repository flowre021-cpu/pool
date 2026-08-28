package com.example.pool.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeTabTest {
    @Test
    fun fromPageIndex_mapsTimelineAndWeekAgenda() {
        assertEquals(HomeTab.TIMELINE, HomeTab.fromPageIndex(0))
        assertEquals(HomeTab.WEEK_AGENDA, HomeTab.fromPageIndex(1))
        assertEquals(HomeTab.WEEK_AGENDA, HomeTab.fromPageIndex(99))
    }

    @Test
    fun displayName_matchesProductLabels() {
        assertEquals("14 天时间轴", HomeTab.TIMELINE.displayName())
        assertEquals("七天事务", HomeTab.WEEK_AGENDA.displayName())
    }
}
