package com.example.pool.ui.affair

import com.example.pool.data.AffairEntity
import com.example.pool.data.AffairType
import com.example.pool.util.ReminderRecurrenceMode
import com.example.pool.util.parseReminderRecurrence
import java.time.Instant
import java.time.ZoneId

data class AffairListSections(
    val active: List<AffairEntity>,
    /** 已完成 + 已过期，按时序排列 */
    val folded: List<AffairEntity>,
    /** 仅时间过期且未完成的数量，用于红字提示 */
    val expiredCount: Int,
)

fun AffairType.foldedSectionTitle(): String = when (this) {
    AffairType.EVENT -> "已结束"
    AffairType.REMINDER,
    AffairType.OPPORTUNITY,
    AffairType.TASK,
    -> "已完成 / 已过期"
}

fun AffairType.showExpiredCountInRed(): Boolean = this != AffairType.EVENT

fun partitionAffairsForList(
    affairs: List<AffairEntity>,
    nowMillis: Long = System.currentTimeMillis(),
): AffairListSections {
    val comparator = compareBy<AffairEntity>({ affairSortMillis(it) }, { it.id })
    val sorted = affairs.sortedWith(comparator)
    val active = mutableListOf<AffairEntity>()
    val expired = mutableListOf<AffairEntity>()
    val completed = mutableListOf<AffairEntity>()
    sorted.forEach { affair ->
        when {
            affair.type.showsDoneCheckbox() && affair.isDone -> completed.add(affair)
            isAffairTimeExpired(affair, nowMillis) -> expired.add(affair)
            else -> active.add(affair)
        }
    }
    val folded = (expired + completed).sortedWith(comparator)
    return AffairListSections(
        active = active,
        folded = folded,
        expiredCount = expired.size,
    )
}

internal fun affairSortMillis(affair: AffairEntity): Long =
    when (affair.type) {
        AffairType.EVENT -> affair.startAt ?: affair.endAt ?: Long.MAX_VALUE
        AffairType.REMINDER -> affair.startAt ?: Long.MAX_VALUE
        AffairType.OPPORTUNITY -> affair.startAt ?: affair.endAt ?: Long.MAX_VALUE
        AffairType.TASK -> affair.deadline ?: Long.MAX_VALUE
    }

internal fun isAffairTimeExpired(affair: AffairEntity, nowMillis: Long): Boolean {
    when (affair.type) {
        AffairType.EVENT -> {
            val end = affair.endAt ?: affair.startAt ?: return false
            return end < nowMillis
        }
        AffairType.REMINDER -> {
            if (hasReminderRecurrence(affair)) return false
            val start = affair.startAt ?: return false
            val zone = ZoneId.systemDefault()
            val reminderDate = Instant.ofEpochMilli(start).atZone(zone).toLocalDate()
            val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
            return reminderDate.isBefore(today)
        }
        AffairType.OPPORTUNITY -> {
            val end = affair.endAt ?: affair.startAt ?: return false
            return end < nowMillis
        }
        AffairType.TASK -> {
            val deadline = affair.deadline ?: return false
            return deadline < nowMillis
        }
    }
}

private fun hasReminderRecurrence(affair: AffairEntity): Boolean =
    parseReminderRecurrence(affair.recurrenceRule).mode != ReminderRecurrenceMode.NONE
