package com.example.pool.ui.affair

import com.example.pool.data.AffairEntity
import com.example.pool.data.AffairType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AffairViewModelTest {
    @Test
    fun mergeEventEdit_updatesVisibleFields_andPreservesStoredMetadata() {
        val existing = AffairEntity(
            id = 7,
            type = AffairType.EVENT,
            title = "旧标题",
            startAt = 100,
            endAt = 200,
            deadline = 300,
            location = "旧地点",
            recurrenceRule = "FREQ=WEEKLY",
            isDone = true,
            note = "旧备注",
            cardColor = 1,
            createdAt = 42,
        )
        val edited = AffairEntity(
            id = 7,
            type = AffairType.EVENT,
            title = "新标题",
            startAt = 400,
            endAt = 500,
            location = null,
            note = null,
            cardColor = 2,
        )

        val merged = mergeAffairEdit(existing, edited)

        assertEquals("新标题", merged.title)
        assertEquals(400L, merged.startAt)
        assertEquals(500L, merged.endAt)
        assertEquals(null, merged.location)
        assertEquals(null, merged.note)
        assertEquals(2L, merged.cardColor)
        assertEquals(300L, merged.deadline)
        assertEquals("FREQ=WEEKLY", merged.recurrenceRule)
        assertTrue(merged.isDone)
        assertEquals(42L, merged.createdAt)
    }

    @Test
    fun mergeOpportunityEdit_updatesColorAndBoundaries() {
        val existing = AffairEntity(
            id = 9,
            type = AffairType.OPPORTUNITY,
            title = "旧周期",
            startAt = 100,
            endAt = 200,
            cardColor = 1,
            createdAt = 10,
        )
        val edited = AffairEntity(
            id = 9,
            type = AffairType.OPPORTUNITY,
            title = "新周期",
            startAt = 300,
            endAt = 400,
            cardColor = 2,
        )

        val merged = mergeAffairEdit(existing, edited)

        assertEquals("新周期", merged.title)
        assertEquals(300L, merged.startAt)
        assertEquals(400L, merged.endAt)
        assertEquals(2L, merged.cardColor)
        assertEquals(10L, merged.createdAt)
    }

    @Test
    fun mergeReminderEdit_preservesRecurrenceAndCompletion() {
        val existing = AffairEntity(
            id = 8,
            type = AffairType.REMINDER,
            title = "提醒",
            startAt = 100,
            recurrenceRule = "FREQ=DAILY",
            isDone = true,
        )
        val edited = AffairEntity(
            id = 8,
            type = AffairType.REMINDER,
            title = "更新提醒",
            startAt = 200,
            recurrenceRule = "WEEKLY:3",
        )

        val merged = mergeAffairEdit(existing, edited)

        assertEquals(200L, merged.startAt)
        assertEquals("WEEKLY:3", merged.recurrenceRule)
        assertTrue(merged.isDone)
    }

    @Test
    fun affairSaveValidationError_opportunityRequiresBoundary() {
        assertEquals(
            "请设置开始或截止时间",
            affairSaveValidationError(
                affairType = AffairType.OPPORTUNITY,
                title = "比赛",
                hasStart = false,
                hasEnd = false,
            ),
        )
        assertEquals(
            null,
            affairSaveValidationError(
                affairType = AffairType.OPPORTUNITY,
                title = "比赛",
                hasStart = true,
                hasEnd = false,
            ),
        )
    }

    @Test
    fun affairSaveValidationError_requiresTitle() {
        assertEquals(
            "请输入标题",
            affairSaveValidationError(
                affairType = AffairType.REMINDER,
                title = "  ",
                hasStart = false,
                hasEnd = false,
            ),
        )
    }
}
