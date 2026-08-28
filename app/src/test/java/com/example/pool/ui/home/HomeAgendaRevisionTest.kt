package com.example.pool.ui.home

import com.example.pool.data.AffairEntity
import com.example.pool.data.AffairType
import com.example.pool.data.schedule.ScheduleCourse
import com.example.pool.data.schedule.ScheduleTimeSlot
import com.example.pool.data.schedule.Semester
import org.junit.Assert.assertNotEquals
import org.junit.Test

class HomeAgendaRevisionTest {
    @Test
    fun contentRevision_changesWhenCourseFieldsChange() {
        val baseCourse = ScheduleCourse(
            id = 1L,
            groupId = "g1",
            name = "数学",
            classroom = null,
            teacher = null,
            note = null,
            dayOfWeek = 1,
            startSection = 1,
            endSection = 2,
            cardColor = 0xFFAA0000,
            selectedWeeks = listOf(1, 2),
        )
        val base = HomeAgendaInputs(courses = listOf(baseCourse))
        val movedDay = HomeAgendaInputs(courses = listOf(baseCourse.copy(dayOfWeek = 2)))

        assertNotEquals(base.contentRevision(), movedDay.contentRevision())
    }

    @Test
    fun contentRevision_changesWhenEventFieldsChange() {
        val event = AffairEntity(
            id = 1L,
            type = AffairType.EVENT,
            title = "会议",
            startAt = 100L,
            endAt = 200L,
        )
        val base = HomeAgendaInputs(events = listOf(event))
        val updated = HomeAgendaInputs(events = listOf(event.copy(endAt = 300L)))

        assertNotEquals(base.contentRevision(), updated.contentRevision())
    }

    @Test
    fun contentRevision_changesWhenSemesterChanges() {
        val semester = Semester(
            id = "2026-spring",
            name = "春季",
            startDateEpochDay = 100L,
            endDateEpochDay = 200L,
            isActive = true,
        )
        val base = HomeAgendaInputs(
            timeSlots = listOf(
                ScheduleTimeSlot(
                    sectionNumber = 1,
                    startTimeMinutes = 480,
                    endTimeMinutes = 525,
                ),
            ),
            semester = semester,
        )
        val extended = base.copy(semester = semester.copy(endDateEpochDay = 250L))

        assertNotEquals(base.contentRevision(), extended.contentRevision())
    }
}
