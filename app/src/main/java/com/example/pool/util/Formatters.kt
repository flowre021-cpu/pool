package com.example.pool.util

import androidx.compose.ui.graphics.Color
import com.example.pool.ui.theme.DdlDueSoon
import com.example.pool.ui.theme.DdlNormal
import com.example.pool.ui.theme.DdlOverdue
import com.example.pool.ui.theme.PoolColors
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

private val deadlineFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")
private val deadlineDateFormatter = DateTimeFormatter.ofPattern("MM-dd")
private val deadlineTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val todayHeaderFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE", Locale.CHINESE)

enum class DeadlineUrgency {
    NONE,
    NORMAL,
    DUE_SOON,
    TODAY,
    OVERDUE,
}

data class DeadlineStatus(
    val urgency: DeadlineUrgency,
    val timeText: String,
    val badgeText: String?,
)

private const val DUE_SOON_HOURS = 72L

fun deadlineUrgency(deadline: Long?, isDone: Boolean): DeadlineUrgency {
    if (isDone || deadline == null) return DeadlineUrgency.NONE
    val zone = ZoneId.systemDefault()
    val now = Instant.now().atZone(zone)
    val ddl = Instant.ofEpochMilli(deadline).atZone(zone)
    if (ddl.toInstant().isBefore(now.toInstant())) return DeadlineUrgency.OVERDUE
    if (ddl.toLocalDate() == now.toLocalDate()) return DeadlineUrgency.TODAY
    val hoursLeft = Duration.between(now.toInstant(), ddl.toInstant()).toHours()
    if (hoursLeft in 0..DUE_SOON_HOURS) return DeadlineUrgency.DUE_SOON
    return DeadlineUrgency.NORMAL
}

fun deadlineStatus(deadline: Long?, isDone: Boolean): DeadlineStatus {
    if (isDone || deadline == null) {
        return DeadlineStatus(
            urgency = DeadlineUrgency.NONE,
            timeText = formatDeadline(deadline),
            badgeText = null,
        )
    }

    val zone = ZoneId.systemDefault()
    val now = Instant.now().atZone(zone)
    val ddl = Instant.ofEpochMilli(deadline).atZone(zone)
    val timeText = formatDeadline(deadline)

    return when (deadlineUrgency(deadline, isDone)) {
        DeadlineUrgency.OVERDUE -> DeadlineStatus(
            urgency = DeadlineUrgency.OVERDUE,
            timeText = timeText,
            badgeText = "已过期",
        )
        DeadlineUrgency.TODAY -> DeadlineStatus(
            urgency = DeadlineUrgency.TODAY,
            timeText = timeText,
            badgeText = "今天到期",
        )
        DeadlineUrgency.DUE_SOON -> {
            val hoursLeft = max(1, Duration.between(now.toInstant(), ddl.toInstant()).toHours())
            DeadlineStatus(
                urgency = DeadlineUrgency.DUE_SOON,
                timeText = timeText,
                badgeText = "剩 ${hoursLeft} 小时",
            )
        }
        DeadlineUrgency.NORMAL -> DeadlineStatus(
            urgency = DeadlineUrgency.NORMAL,
            timeText = timeText,
            badgeText = null,
        )
        DeadlineUrgency.NONE -> DeadlineStatus(
            urgency = DeadlineUrgency.NONE,
            timeText = timeText,
            badgeText = null,
        )
    }
}

fun deadlineColor(urgency: DeadlineUrgency): Color = when (urgency) {
    DeadlineUrgency.OVERDUE -> DdlOverdue
    DeadlineUrgency.TODAY, DeadlineUrgency.DUE_SOON -> DdlDueSoon
    DeadlineUrgency.NORMAL -> DdlNormal
    DeadlineUrgency.NONE -> PoolColors.TextSecondary
}

fun formatDeadline(millis: Long?): String {
    if (millis == null) return "无截止"
    return Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .format(deadlineFormatter)
}

fun formatDeadlineDate(millis: Long): String =
    Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .format(deadlineDateFormatter)

fun formatDeadlineTime(millis: Long): String =
    Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .format(deadlineTimeFormatter)

fun formatTodayHeader(): String = LocalDate.now().format(todayHeaderFormatter)

fun formatDateHeader(date: LocalDate): String = date.format(todayHeaderFormatter)

fun formatWeekRangeHeader(dates: List<LocalDate>): String {
    if (dates.isEmpty()) return formatTodayHeader()
    val start = dates.first()
    val end = dates.last()
    return if (start.year == end.year && start.month == end.month) {
        "${start.year}年${start.monthValue}月${start.dayOfMonth}日 – ${end.dayOfMonth}日"
    } else if (start.year == end.year) {
        "${start.year}年${start.monthValue}月${start.dayOfMonth}日 – ${end.monthValue}月${end.dayOfMonth}日"
    } else {
        "${start.year}年${start.monthValue}月${start.dayOfMonth}日 – ${end.year}年${end.monthValue}月${end.dayOfMonth}日"
    }
}

/** 主页事务表顶部日期：1 天显示单日，3/7 天显示区间 */
fun formatAgendaPeriodHeader(dates: List<LocalDate>, visibleDayCount: Int): String {
    if (dates.isEmpty()) return formatTodayHeader()
    return if (visibleDayCount <= 1) {
        formatDateHeader(dates.first())
    } else {
        formatWeekRangeHeader(dates)
    }
}

fun formatMinutesOfDay(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return "%02d:%02d".format(hours, mins)
}

/** Material DatePicker 的 selectedDateMillis 按 UTC 午夜解释，勿用系统时区 atStartOfDay */
fun localDateToDatePickerUtcMillis(date: LocalDate): Long =
    date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

fun datePickerUtcMillisToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

private val datePickerHeadlineFormatter =
    DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE", Locale.CHINESE)

fun formatDatePickerHeadline(selectedDateMillis: Long?): String =
    selectedDateMillis?.let { datePickerUtcMillisToLocalDate(it).format(datePickerHeadlineFormatter) }
        ?: "选定的日期"

fun formatMinutesOfDayCompact(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return if (mins == 0) "$hours:00" else "%d:%02d".format(hours, mins)
}

fun formatBlockTimeRange(startMinutes: Int, endMinutes: Int): String =
    "${formatMinutesOfDayCompact(startMinutes)}-${formatMinutesOfDayCompact(endMinutes)}"

fun formatCurrentTimeLabel(): String {
    val now = java.time.LocalTime.now()
    return "%02d:%02d".format(now.hour, now.minute)
}

fun dayOfWeekLabel(dayOfWeek: Int): String = when (dayOfWeek) {
    1 -> "周一"
    2 -> "周二"
    3 -> "周三"
    4 -> "周四"
    5 -> "周五"
    6 -> "周六"
    7 -> "周日"
    else -> "未知"
}

fun dayOfWeekShort(dayOfWeek: Int): String = when (dayOfWeek) {
    1 -> "一"
    2 -> "二"
    3 -> "三"
    4 -> "四"
    5 -> "五"
    6 -> "六"
    7 -> "日"
    else -> "?"
}

fun formatWeekRange(weekStart: Int, weekEnd: Int): String = "第 $weekStart - $weekEnd 周"

fun formatSectionRange(start: Int, end: Int): String =
    if (start == end) "第 $start 节" else "第 $start - $end 节"

fun formatScheduleSlot(dayOfWeek: Int, startSection: Int, endSection: Int): String =
    "${dayOfWeekLabel(dayOfWeek)} ${formatSectionRange(startSection, endSection)}"

private val affairDateFormatter = DateTimeFormatter.ofPattern("MM-dd")
private val eventDateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")

fun formatAffairDate(millis: Long?): String {
    if (millis == null) return "未设置"
    return Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .format(affairDateFormatter)
}

/** 开始边界：00:00 视为仅日期 */
fun isAffairStartDateOnly(millis: Long, zone: ZoneId = ZoneId.systemDefault()): Boolean =
    Instant.ofEpochMilli(millis).atZone(zone).toLocalTime() == LocalTime.MIDNIGHT

/** 截止边界：23:59 视为仅日期 */
fun isAffairEndDateOnly(millis: Long, zone: ZoneId = ZoneId.systemDefault()): Boolean =
    Instant.ofEpochMilli(millis).atZone(zone).toLocalTime() == LocalTime.of(23, 59)

fun formatAffairBoundary(
    millis: Long?,
    isEndBoundary: Boolean = false,
    zone: ZoneId = ZoneId.systemDefault(),
): String {
    if (millis == null) return "未设置"
    val zdt = Instant.ofEpochMilli(millis).atZone(zone)
    val dateOnly = if (isEndBoundary) isAffairEndDateOnly(millis, zone) else isAffairStartDateOnly(millis, zone)
    return if (dateOnly) {
        zdt.format(affairDateFormatter)
    } else {
        zdt.format(eventDateTimeFormatter)
    }
}

fun formatOpportunityRange(startMillis: Long?, endMillis: Long?): String {
    if (startMillis == null && endMillis == null) return "未设置"
    if (startMillis != null && endMillis == null) {
        return formatAffairBoundary(startMillis, isEndBoundary = false)
    }
    if (startMillis == null) {
        return "截止 ${formatAffairBoundary(endMillis, isEndBoundary = true)}"
    }
    if (startMillis == endMillis) {
        return formatAffairBoundary(startMillis, isEndBoundary = false)
    }
    val zone = ZoneId.systemDefault()
    val start = Instant.ofEpochMilli(startMillis).atZone(zone)
    val end = Instant.ofEpochMilli(endMillis!!).atZone(zone)
    val startText = formatAffairBoundary(startMillis, isEndBoundary = false, zone)
    val endText = formatAffairBoundary(endMillis, isEndBoundary = true, zone)
    return if (start.toLocalDate() == end.toLocalDate()) {
        val startTimeOnly = !isAffairStartDateOnly(startMillis, zone)
        val endTimeOnly = !isAffairEndDateOnly(endMillis, zone)
        when {
            startTimeOnly && endTimeOnly ->
                "${start.format(affairDateFormatter)} ${start.format(deadlineTimeFormatter)} ~ ${end.format(deadlineTimeFormatter)}"
            else -> "$startText ~ $endText"
        }
    } else {
        "$startText ~ $endText"
    }
}

/** 提醒列表/编辑：无日程时显示「随时提示」 */
fun formatReminderSchedule(startAt: Long?, zone: ZoneId = ZoneId.systemDefault()): String {
    if (startAt == null) return "随时提示"
    val zdt = Instant.ofEpochMilli(startAt).atZone(zone)
    return if (isAffairStartDateOnly(startAt, zone)) {
        formatAffairDate(startAt)
    } else {
        "${zdt.format(affairDateFormatter)} ${zdt.format(deadlineTimeFormatter)}"
    }
}

fun combineAffairDateAndTime(
    date: LocalDate,
    time: LocalTime,
    zone: ZoneId = ZoneId.systemDefault(),
): Long = LocalDateTime.of(date, time).atZone(zone).toInstant().toEpochMilli()

fun localDateFromMillis(millis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

fun localTimeFromMillis(millis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalTime =
    Instant.ofEpochMilli(millis).atZone(zone).toLocalTime()

fun applyDateToBoundaryMillis(
    pickedDate: LocalDate,
    existingMillis: Long?,
    isEndBoundary: Boolean,
    zone: ZoneId = ZoneId.systemDefault(),
): Long {
    val time = existingMillis?.let { localTimeFromMillis(it, zone) }
    return when {
        time != null && isEndBoundary && isAffairEndDateOnly(existingMillis, zone) ->
            endOfDayMillis(pickedDate, zone)
        time != null && !isEndBoundary && isAffairStartDateOnly(existingMillis, zone) ->
            startOfDayMillis(pickedDate, zone)
        time != null ->
            combineAffairDateAndTime(pickedDate, time, zone)
        isEndBoundary -> endOfDayMillis(pickedDate, zone)
        else -> startOfDayMillis(pickedDate, zone)
    }
}

fun formatEventDateTime(millis: Long?): String {
    if (millis == null) return "未设置"
    return Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .format(eventDateTimeFormatter)
}

fun formatEventTimeRange(startMillis: Long?, endMillis: Long?): String {
    if (startMillis == null && endMillis == null) return "未设置"
    if (startMillis == null) return formatEventDateTime(endMillis)
    if (endMillis == null) return formatEventDateTime(startMillis)
    val zone = ZoneId.systemDefault()
    val start = Instant.ofEpochMilli(startMillis).atZone(zone)
    val end = Instant.ofEpochMilli(endMillis).atZone(zone)
    val startText = start.format(eventDateTimeFormatter)
    return if (start.toLocalDate() == end.toLocalDate()) {
        "$startText ~ ${end.format(deadlineTimeFormatter)}"
    } else {
        "$startText ~ ${end.format(eventDateTimeFormatter)}"
    }
}

fun formatEventLocation(location: String?): String =
    if (location.isNullOrBlank()) "无地点" else "@$location"

/** Reminder / 全天事务：相对今天的标签 */
fun affairDayLabel(millis: Long?, isDone: Boolean): String? {
    if (isDone || millis == null) return null
    val zone = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
    val today = LocalDate.now(zone)
    return when {
        date.isBefore(today) -> "已过期"
        date == today -> "今天"
        date == today.plusDays(1) -> "明天"
        else -> null
    }
}

fun startOfDayMillis(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long =
    date.atStartOfDay(zone).toInstant().toEpochMilli()

fun endOfDayMillis(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long =
    date.atTime(23, 59).atZone(zone).toInstant().toEpochMilli()
