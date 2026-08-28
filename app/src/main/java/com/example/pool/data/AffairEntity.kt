package com.example.pool.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "affairs",
    indices = [Index(value = ["type"])],
)
data class AffairEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: AffairType,
    val title: String,
    val startAt: Long? = null,
    val endAt: Long? = null,
    val deadline: Long? = null,
    val location: String? = null,
    /** 预留：Reminder 重复规则（如 RRULE 或 JSON），v0.1 可为空 */
    val recurrenceRule: String? = null,
    val isDone: Boolean = false,
    /** 循环提醒已完成的 occurrence（epoch day，逗号分隔）；非循环提醒不使用 */
    val completedOccurrenceDays: String? = null,
    val note: String? = null,
    /** Event 卡片颜色（ARGB Long），与课表 cardColor 同格式 */
    val cardColor: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
