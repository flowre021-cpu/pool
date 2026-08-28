package com.example.pool.ui.affair

import com.example.pool.data.AffairType
import com.example.pool.util.ReminderRecurrence
import com.example.pool.util.reminderRecurrenceValidationError

fun AffairType.displayName(): String = hubTitle()

fun AffairType.hubTitle(): String = when (this) {
    AffairType.TASK -> "待办"
    AffairType.OPPORTUNITY -> "周期事项"
    AffairType.EVENT -> "日程事件"
    AffairType.REMINDER -> "提醒"
}

fun AffairType.hubSubtitle(): String = when (this) {
    AffairType.TASK -> "管理待办事项与 DDL 截止时间"
    AffairType.OPPORTUNITY -> "记录比赛、活动、申请与关键时间区间"
    AffairType.EVENT -> "临时日程占用，固定开始与结束时间"
    AffairType.REMINDER -> "轻量提示，支持每天/每周循环"
}

fun AffairType.addActionLabel(): String = "添加${hubTitle()}"

fun AffairType.listEmptyHint(): String = "暂无${hubTitle()}，点右下角 + 添加"

/** Event 仅作提示，不提供完成勾选 */
fun AffairType.showsDoneCheckbox(): Boolean = this != AffairType.EVENT

fun AffairType.doneCheckboxLabel(): String = when (this) {
    AffairType.TASK -> "完成"
    AffairType.OPPORTUNITY -> "完成"
    AffairType.REMINDER -> "完成"
    AffairType.EVENT -> ""
}

fun AffairType.editTitle(isNew: Boolean): String = when (this) {
    AffairType.TASK -> if (isNew) "新建待办" else "编辑待办"
    AffairType.OPPORTUNITY -> if (isNew) "新建周期事项" else "编辑周期事项"
    AffairType.EVENT -> if (isNew) "新建日程事件" else "编辑日程事件"
    AffairType.REMINDER -> if (isNew) "新建提醒" else "编辑提醒"
}

/** @return 校验失败时的提示文案；通过则 null */
fun affairSaveValidationError(
    affairType: AffairType,
    title: String,
    hasStart: Boolean,
    hasEnd: Boolean,
    reminderRecurrence: ReminderRecurrence = ReminderRecurrence(),
): String? {
    if (title.isBlank()) return "请输入标题"
    reminderRecurrenceValidationError(reminderRecurrence)?.let { return it }
    return when (affairType) {
        AffairType.REMINDER -> null
        AffairType.OPPORTUNITY ->
            if (hasStart || hasEnd) null else "请设置开始或截止时间"
        AffairType.EVENT -> when {
            !hasStart && !hasEnd -> "请设置日期与时间段"
            !hasStart -> "请设置开始时间"
            !hasEnd -> "请设置结束时间"
            else -> null
        }
        AffairType.TASK -> null
    }
}

val AffairHubTypes = listOf(
    AffairType.TASK,
    AffairType.OPPORTUNITY,
    AffairType.EVENT,
    AffairType.REMINDER,
)
