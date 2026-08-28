package com.example.pool.ui.home

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AgendaBehaviorTest {
    @Test
    fun threeDayView_alwaysUsesThreeColumnsAtWeekend() {
        assertEquals(
            listOf(4, 5, 6),
            visibleDayIndices(totalDays = 7, todayIndex = 6, zoomLevel = AgendaZoomLevel.THREE),
        )
    }

    @Test
    fun eventEndingAtMidnight_doesNotAppearOnFollowingDay() {
        val zone = ZoneId.systemDefault()
        val nextDay = LocalDate.of(2026, 8, 5)
        val start = LocalDate.of(2026, 8, 4)
            .atTime(23, 0)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
        val end = nextDay
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()

        assertFalse(eventOverlapsDay(start, end, nextDay))
        assertEquals(
            DAY_END_MINUTES to DAY_END_MINUTES,
            clipToDayMinutes(start, end, nextDay, zone),
        )
    }

    @Test
    fun resolvePageAnchor_mapsRelativePageOffset() {
        val anchor = LocalDate.of(2026, 8, 4)
        assertEquals(
            LocalDate.of(2026, 8, 11),
            resolvePageAnchor(
                displayAnchor = anchor,
                referencePage = 100,
                pageIndex = 101,
                periodDays = AgendaZoomLevel.WEEK.visibleDays,
            ),
        )
        assertEquals(
            anchor,
            resolvePageAnchor(
                displayAnchor = anchor,
                referencePage = 100,
                pageIndex = 100,
                periodDays = AgendaZoomLevel.WEEK.visibleDays,
            ),
        )
    }

    @Test
    fun pageStateCache_reusesBuiltState() {
        val cache = AgendaPageStateCache(maxSize = 8)
        val inputs = HomeAgendaInputs()
        val anchor = LocalDate.of(2026, 8, 4)

        val first = cache.getFullWeekOrBuild(inputs, anchor)
        val second = cache.getFullWeekOrBuild(inputs, anchor)

        assertEquals(first, second)
    }

    @Test
    fun pageStateCache_invalidatesWhenInputsChange() {
        val cache = AgendaPageStateCache(maxSize = 8)
        val anchor = LocalDate.of(2026, 8, 4)
        val emptyInputs = HomeAgendaInputs()
        val slot = com.example.pool.data.schedule.ScheduleTimeSlot(
            sectionNumber = 1,
            startTimeMinutes = 8 * 60,
            endTimeMinutes = 8 * 60 + 45,
        )

        cache.getFullWeekOrBuild(emptyInputs, anchor)
        val withData = HomeAgendaInputs(timeSlots = listOf(slot))
        val rebuilt = cache.getFullWeekOrBuild(withData, anchor)

        assertEquals(listOf(slot.sectionNumber), rebuilt.timeSlots.map { it.sectionNumber })
    }

    @Test
    fun resolvePagerWeekState_reusesCurrentWeekForSameWeekAnchor() {
        val monday = LocalDate.of(2026, 8, 3)
        val wednesday = LocalDate.of(2026, 8, 5)
        val weekDates = (0..6).map { monday.plusDays(it.toLong()) }
        val current = HomeUiState(
            anchorDate = monday,
            weekDates = weekDates,
            dayColumns = weekDates.map { date ->
                AgendaDayColumn(date = date, dayOfWeek = date.dayOfWeek.value, blocks = emptyList())
            },
        )

        val resolved = resolvePagerWeekState(
            pageAnchor = wednesday,
            currentWeek = current,
            weekStatesByAnchor = emptyMap(),
        )

        assertEquals(wednesday, resolved.anchorDate)
        assertEquals(weekDates, resolved.weekDates)
    }

    @Test
    fun agendaScrollCenterY_usesBlockSpanNotCurrentTime() {
        val blocks = listOf(
            TimedLayoutEntry("a", startMinutes = 9 * 60, endMinutes = 10 * 60),
            TimedLayoutEntry("b", startMinutes = 14 * 60, endMinutes = 16 * 60),
        )
        val center = agendaScrollCenterY(
            blocks = blocks,
            useSectionGrid = false,
            timeSlots = emptyList(),
        )
        // 事务跨度 9:00–16:00，中心 12:30 → 12.5 × 48dp
        assertEquals(600f, center.value, 0.01f)
    }

    @Test
    fun agendaScrollCenterY_emptyDayDefaultsToDaytimeWindow() {
        val center = agendaScrollCenterY(
            blocks = emptyList(),
            useSectionGrid = false,
            timeSlots = emptyList(),
        )
        // 8:00–18:00 中心 13:00 → 13 × 48dp
        assertEquals(624f, center.value, 0.01f)
    }
}
