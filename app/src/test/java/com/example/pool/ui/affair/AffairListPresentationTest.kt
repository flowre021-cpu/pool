package com.example.pool.ui.affair

import com.example.pool.data.AffairEntity
import com.example.pool.data.AffairType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class AffairListPresentationTest {
    @Test
    fun partitionAffairsForList_sortsEventsChronologically() {
        val later = AffairEntity(
            id = 2L,
            type = AffairType.EVENT,
            title = "下午",
            startAt = 500L,
            endAt = 600L,
        )
        val earlier = AffairEntity(
            id = 1L,
            type = AffairType.EVENT,
            title = "上午",
            startAt = 100L,
            endAt = 200L,
        )

        val sections = partitionAffairsForList(
            affairs = listOf(later, earlier),
            nowMillis = 50L,
        )

        assertEquals(listOf("上午", "下午"), sections.active.map { it.title })
        assertTrue(sections.folded.isEmpty())
        assertEquals(0, sections.expiredCount)
    }

    @Test
    fun partitionAffairsForList_movesEndedEventsToFoldedSection() {
        val ended = AffairEntity(
            id = 1L,
            type = AffairType.EVENT,
            title = "已结束",
            startAt = 100L,
            endAt = 200L,
        )
        val upcoming = AffairEntity(
            id = 2L,
            type = AffairType.EVENT,
            title = "未开始",
            startAt = 500L,
            endAt = 600L,
        )

        val sections = partitionAffairsForList(
            affairs = listOf(ended, upcoming),
            nowMillis = 300L,
        )

        assertEquals(listOf("未开始"), sections.active.map { it.title })
        assertEquals(listOf("已结束"), sections.folded.map { it.title })
        assertEquals(1, sections.expiredCount)
    }

    @Test
    fun partitionAffairsForList_mergesCompletedAndExpiredWithSeparateCount() {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.of(2026, 8, 7)
        val nowMillis = today.atTime(14, 0).atZone(zone).toInstant().toEpochMilli()
        val expired = AffairEntity(
            id = 1L,
            type = AffairType.REMINDER,
            title = "过期",
            startAt = today.minusDays(1).atTime(9, 0).atZone(zone).toInstant().toEpochMilli(),
        )
        val completed = AffairEntity(
            id = 2L,
            type = AffairType.REMINDER,
            title = "完成",
            startAt = today.atTime(9, 0).atZone(zone).toInstant().toEpochMilli(),
            isDone = true,
        )

        val sections = partitionAffairsForList(
            affairs = listOf(completed, expired),
            nowMillis = nowMillis,
        )

        assertTrue(sections.active.isEmpty())
        assertEquals(listOf("过期", "完成"), sections.folded.map { it.title })
        assertEquals(1, sections.expiredCount)
    }

    @Test
    fun partitionAffairsForList_keepsRecurringReminderActiveAfterStartTime() {
        val reminder = AffairEntity(
            id = 1L,
            type = AffairType.REMINDER,
            title = "每天提醒",
            startAt = 100L,
            recurrenceRule = "DAILY",
        )

        val sections = partitionAffairsForList(
            affairs = listOf(reminder),
            nowMillis = 500L,
        )

        assertEquals(listOf("每天提醒"), sections.active.map { it.title })
        assertTrue(sections.folded.isEmpty())
    }

    @Test
    fun partitionAffairsForList_keepsTodayReminderActiveAfterStartTime() {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.of(2026, 8, 7)
        val startAt = today.atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        val nowMillis = today.atTime(14, 0).atZone(zone).toInstant().toEpochMilli()
        val reminder = AffairEntity(
            id = 1L,
            type = AffairType.REMINDER,
            title = "今日提醒",
            startAt = startAt,
        )

        val sections = partitionAffairsForList(
            affairs = listOf(reminder),
            nowMillis = nowMillis,
        )

        assertEquals(listOf("今日提醒"), sections.active.map { it.title })
        assertTrue(sections.folded.isEmpty())
        assertEquals(0, sections.expiredCount)
    }

    @Test
    fun partitionAffairsForList_movesYesterdayReminderToFolded() {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.of(2026, 8, 7)
        val startAt = today.minusDays(1).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        val nowMillis = today.atTime(14, 0).atZone(zone).toInstant().toEpochMilli()
        val reminder = AffairEntity(
            id = 1L,
            type = AffairType.REMINDER,
            title = "昨日提醒",
            startAt = startAt,
        )

        val sections = partitionAffairsForList(
            affairs = listOf(reminder),
            nowMillis = nowMillis,
        )

        assertTrue(sections.active.isEmpty())
        assertEquals(listOf("昨日提醒"), sections.folded.map { it.title })
        assertEquals(1, sections.expiredCount)
    }

    @Test
    fun partitionAffairsForList_prefersCompletedOverExpiredWhenBothApply() {
        val task = AffairEntity(
            id = 1L,
            type = AffairType.TASK,
            title = "已勾选",
            deadline = 100L,
            isDone = true,
        )

        val sections = partitionAffairsForList(
            affairs = listOf(task),
            nowMillis = 500L,
        )

        assertEquals(0, sections.expiredCount)
        assertEquals(listOf("已勾选"), sections.folded.map { it.title })
    }
}
