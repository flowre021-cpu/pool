package com.example.pool.widget

import com.example.pool.data.AffairEntity
import com.example.pool.data.AffairType
import com.example.pool.data.schedule.ScheduleCourse
import com.example.pool.data.schedule.ScheduleTimeSlot
import com.example.pool.data.schedule.Semester
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class WidgetDataLoaderTest {
    private val today = LocalDate.of(2026, 8, 6) // Thursday
    private val semester = Semester(
        id = "2026-spring",
        name = "2026 春",
        startDateEpochDay = LocalDate.of(2026, 2, 23).toEpochDay(),
        endDateEpochDay = LocalDate.of(2026, 12, 31).toEpochDay(),
        isActive = true,
    )

    @Test
    fun buildTodayScheduleItems_prefersUpcomingCourseAndEvent() {
        val zone = ZoneId.systemDefault()
        val course = ScheduleCourse(
            id = 1L,
            groupId = "g1",
            name = "数学课",
            classroom = "A101",
            teacher = null,
            note = null,
            dayOfWeek = today.dayOfWeek.value,
            startSection = 3,
            endSection = 4,
            cardColor = 0xFF98B8E8L,
            selectedWeeks = listOf(
                com.example.pool.util.SemesterCalendar.weekNumberForDate(
                    today,
                    com.example.pool.util.SemesterCalendar.startDate(semester),
                ),
            ),
        )
        val slots = listOf(
            ScheduleTimeSlot(3, 14 * 60, 14 * 60 + 45),
            ScheduleTimeSlot(4, 14 * 60 + 50, 15 * 60 + 35),
        )
        val eventStart = today.atTime(17, 0).atZone(zone).toInstant().toEpochMilli()
        val eventEnd = today.atTime(17, 55).atZone(zone).toInstant().toEpochMilli()
        val event = AffairEntity(
            id = 9L,
            type = AffairType.EVENT,
            title = "组会",
            startAt = eventStart,
            endAt = eventEnd,
            location = "线上",
        )

        val items = WidgetDataLoader.buildTodayScheduleItems(
            today = today,
            courses = listOf(course),
            timeSlots = slots,
            semester = semester,
            events = listOf(event),
        )

        assertEquals(2, items.size)
        assertEquals("数学课", items[0].title)
        assertEquals("A101", items[0].location)
        assertEquals("组会", items[1].title)
    }

    @Test
    fun buildTodayScheduleItems_includesAllTodayEvents() {
        val zone = ZoneId.systemDefault()
        val eventStart = today.atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        val eventEnd = today.atTime(10, 0).atZone(zone).toInstant().toEpochMilli()
        val event = AffairEntity(
            id = 1L,
            type = AffairType.EVENT,
            title = "早会",
            startAt = eventStart,
            endAt = eventEnd,
        )

        val items = WidgetDataLoader.buildTodayScheduleItems(
            today = today,
            courses = emptyList(),
            timeSlots = emptyList(),
            semester = null,
            events = listOf(event),
        )

        assertEquals(1, items.size)
        assertEquals("早会", items.single().title)
    }

    @Test
    fun buildDeadlineItems_sortsByNearestDeadline() {
        val zone = ZoneId.systemDefault()
        val referenceNow = today.atTime(10, 0).atZone(zone).toInstant().toEpochMilli()
        val soon = today.plusDays(1).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val later = today.plusDays(3).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val tasks = listOf(
            AffairEntity(id = 1L, type = AffairType.TASK, title = "报告", deadline = soon),
            AffairEntity(id = 2L, type = AffairType.TASK, title = "论文", deadline = later),
        )

        val items = WidgetDataLoader.buildDeadlineItems(tasks, referenceNow)

        assertEquals("报告", items.first().title)
        assertEquals("1日", items.first().remainingLabel)
    }

    @Test
    fun buildDeadlineItems_hidesDoneTask() {
        val zone = ZoneId.systemDefault()
        val deadline = today.atTime(18, 0).atZone(zone).toInstant().toEpochMilli()
        val task = AffairEntity(
            id = 5L,
            type = AffairType.TASK,
            title = "已完成",
            deadline = deadline,
            isDone = true,
        )

        val items = WidgetDataLoader.buildDeadlineItems(tasks = listOf(task))

        assertTrue(items.isEmpty())
    }
}
