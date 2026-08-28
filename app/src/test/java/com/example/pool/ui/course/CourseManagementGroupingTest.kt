package com.example.pool.ui.course

import com.example.pool.data.schedule.CourseGroupSummary
import com.example.pool.data.schedule.Semester
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseManagementGroupingTest {
    @Test
    fun groupCourseGroupsBySemester_sortsByStartDateDesc() {
        val semesters = listOf(
            Semester("2026-spring", "2026春季学期", 100, 200, true),
            Semester("2025-fall", "2025秋季学期", 10, 90, false),
        )
        val groups = listOf(
            CourseGroupSummary("g1", "数学", 1, 2, "2025-fall"),
            CourseGroupSummary("g2", "英语", 2, 1, "2026-spring"),
        )
        val sections = groupCourseGroupsBySemester(groups, semesters)
        assertEquals("2026春季学期", sections.first().title)
        assertEquals(1, sections.first().groups.size)
    }

    @Test
    fun groupCourseGroupsBySemester_legacyBucket() {
        val sections = groupCourseGroupsBySemester(
            listOf(CourseGroupSummary("g1", "旧课", 1, 1, null)),
            emptyList(),
        )
        assertEquals("未归属学期", sections.single().title)
        assertTrue(sections.single().groups.single().name == "旧课")
    }
}
