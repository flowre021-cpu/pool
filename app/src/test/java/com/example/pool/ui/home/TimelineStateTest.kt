package com.example.pool.ui.home

import com.example.pool.data.AffairEntity
import com.example.pool.data.AffairType
import com.example.pool.util.ReminderRecurrenceMode
import java.time.LocalDate
import java.time.ZoneId
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineStateTest {
    private val today = LocalDate.of(2026, 8, 5)

    @Test
    fun buildTimelineUiState_mapsTaskToDeadlineDay() {
        val deadline = today.plusDays(2)
            .atTime(18, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val tasks = listOf(
            AffairEntity(
                id = 1L,
                type = AffairType.TASK,
                title = "作业",
                deadline = deadline,
            ),
        )

        val state = buildTimelineUiState(
            tasks = tasks,
            opportunities = emptyList(),
            reminders = emptyList(),
            showReminders = false,
            today = today,
        )

        assertEquals(1, state.taskMarkers.size)
        assertEquals(2, state.taskMarkers.first().dayIndex)
        assertEquals(1, state.days[2].taskCount)
    }

    @Test
    fun buildTimelineUiState_excludesCompletedTasks() {
        val deadline = today.plusDays(1)
            .atTime(18, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val tasks = listOf(
            AffairEntity(
                id = 1L,
                type = AffairType.TASK,
                title = "已完成",
                deadline = deadline,
                isDone = true,
            ),
        )

        val state = buildTimelineUiState(
            tasks = tasks,
            opportunities = emptyList(),
            reminders = emptyList(),
            showReminders = false,
            today = today,
        )

        assertTrue(state.taskMarkers.isEmpty())
        assertEquals(0, state.days[1].taskCount)
        assertTrue(state.dayDetailsByDate[today.plusDays(1)]?.tasks.isNullOrEmpty())
    }

    @Test
    fun buildTimelineUiState_opportunitySpansMultipleDays() {
        val start = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = today.plusDays(3)
            .atTime(23, 59)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val opportunities = listOf(
            AffairEntity(
                id = 2L,
                type = AffairType.OPPORTUNITY,
                title = "申请窗口",
                startAt = start,
                endAt = end,
            ),
        )

        val state = buildTimelineUiState(
            tasks = emptyList(),
            opportunities = opportunities,
            reminders = emptyList(),
            showReminders = false,
            today = today,
        )

        assertEquals(1, state.opportunitySegments.size)
        val segment = state.opportunitySegments.first()
        assertEquals(0, segment.startDayIndex)
        assertEquals(3, segment.endDayIndex)
        assertEquals(1, state.days[0].opportunityCount)
        assertEquals(1, state.dayDetailsByDate[today]?.opportunities?.size)
    }

    @Test
    fun buildTimelineUiState_remindersHiddenByDefault() {
        val reminders = listOf(
            AffairEntity(
                id = 3L,
                type = AffairType.REMINDER,
                title = "每日提醒",
                recurrenceRule = "DAILY",
            ),
        )

        val hidden = buildTimelineUiState(
            tasks = emptyList(),
            opportunities = emptyList(),
            reminders = reminders,
            showReminders = false,
            today = today,
        )
        assertTrue(hidden.reminderMarkers.isEmpty())

        val shown = buildTimelineUiState(
            tasks = emptyList(),
            opportunities = emptyList(),
            reminders = reminders,
            showReminders = true,
            today = today,
        )
        assertEquals(TIMELINE_DAY_COUNT, shown.reminderMarkers.size)
    }

    @Test
    fun reminderOccurrencesInWindow_weeklyMatchesSelectedDays() {
        val window = (0 until 7).map { today.plusDays(it.toLong()) }
        val reminder = AffairEntity(
            id = 4L,
            type = AffairType.REMINDER,
            title = "每周一",
            recurrenceRule = "WEEKLY:1",
        )

        val occurrences = reminderOccurrencesInWindow(reminder, window, today)
        assertFalse(occurrences.isEmpty())
        assertTrue(occurrences.all { it.dayOfWeek.value == 1 })
    }

    @Test
    fun buildTimelineDayDetail_groupsAffairsForDate() {
        val deadline = today.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val tasks = listOf(
            AffairEntity(id = 1L, type = AffairType.TASK, title = "A", deadline = deadline),
        )
        val detail = buildTimelineDayDetail(
            date = today,
            tasks = tasks,
            opportunities = emptyList(),
            reminders = emptyList(),
            showReminders = false,
            today = today,
        )
        assertEquals(1, detail.tasks.size)
        assertEquals("A", detail.tasks.first().title)
    }

    @Test
    fun staggerTimelineTaskMarkers_assignsDistinctOffsetsForSameTime() {
        val markers = listOf(
            TimelineTaskMarker(3L, "C", 0, 0.5f, Color.Green),
            TimelineTaskMarker(1L, "A", 0, 0.5f, Color.Red),
            TimelineTaskMarker(2L, "B", 0, 0.5f, Color.Blue),
        )
        val staggered = staggerTimelineTaskMarkers(markers)
        assertEquals(3, staggered.size)
        assertEquals(listOf(0, 1, 2), staggered.sortedBy { it.id }.map { it.offsetIndex })
        staggered.forEach { assertEquals(3, it.groupSize) }
    }

    @Test
    fun staggerTimelineReminderMarkers_keepsSingleMarkerCentered() {
        val markers = listOf(
            TimelineReminderMarker(1L, "R", 2, 0.25f),
        )
        val staggered = staggerTimelineReminderMarkers(markers)
        assertEquals(0, staggered.first().offsetIndex)
        assertEquals(1, staggered.first().groupSize)
    }

    @Test
    fun staggeredMarkerY_offsetsAroundBaseTime() {
        val dayHeightPx = 100f
        val staggerStepPx = 10f
        val low = staggeredMarkerY(0, 0.5f, 0, 3, dayHeightPx, staggerStepPx)
        val mid = staggeredMarkerY(0, 0.5f, 1, 3, dayHeightPx, staggerStepPx)
        val high = staggeredMarkerY(0, 0.5f, 2, 3, dayHeightPx, staggerStepPx)
        assertTrue(low < mid)
        assertTrue(mid < high)
    }

    @Test
    fun assignOpportunityLanes_separatesOverlappingSegments() {
        val segments = listOf(
            TimelineOpportunitySegment(1L, "A", 0, 5, color = Color.Red),
            TimelineOpportunitySegment(2L, "B", 3, 8, color = Color.Blue),
        )
        val assigned = assignOpportunityLanes(segments)
        assertEquals(0, assigned[0].laneIndex)
        assertEquals(1, assigned[1].laneIndex)
    }

    @Test
    fun assignOpportunityLanes_capsAtMaxLanes() {
        val segments = (1L..14L).map { id ->
            TimelineOpportunitySegment(id, "O$id", 0, 10, color = Color.Gray)
        }
        val assigned = assignOpportunityLanes(segments)
        assertTrue(assigned.all { it.laneIndex < MAX_OPPORTUNITY_LANES })
        assertEquals(MAX_OPPORTUNITY_LANES - 1, assigned.last().laneIndex)
    }

    @Test
    fun computeTimelineLaneWidthPx_keepsPreferredWhenSpaceAllows() {
        assertEquals(50f, computeTimelineLaneWidthPx(8, 500f, 50f))
    }

    @Test
    fun computeTimelineLaneWidthPx_shrinksUniformlyWhenOverflow() {
        assertEquals(40f, computeTimelineLaneWidthPx(10, 400f, 50f))
    }

    @Test
    fun timeFractionOfDay_mapsMinutesWithinDay() {
        val noon = LocalDate.of(2026, 8, 5)
            .atTime(12, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        assertEquals(0.5f, timeFractionOfDay(noon), 0.01f)
    }
}
