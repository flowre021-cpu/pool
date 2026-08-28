package com.example.pool.data.schedule.import

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** 北航本研教育管理系统（正方）按周课表 API 解析。 */
object BuaaZhengfangParser {
    private const val API_PATH_SUFFIX = "getMyScheduleDetail.do"

    fun apiPathSuffix(): String = API_PATH_SUFFIX

    /**
     * 合并多周 [weekResponses]（元素为 `{ week, body }`，body 为接口原始 JSON 字符串）。
     * 同一教学班 + 星期 + 节次合并 [selectedWeeks]。
     */
    fun mergeWeeklyResponses(weekResponses: List<WeeklyResponse>): List<ImportedCourseSlot> {
        val merged = linkedMapOf<String, ImportedCourseSlot>()
        for (weekly in weekResponses) {
            val root = JSONObject(weekly.body)
            if (root.optString("code") != "0") {
                throw IllegalStateException(root.optString("msg", "第 ${weekly.week} 周请求失败"))
            }
            val arranged = root.optJSONObject("datas")?.optJSONArray("arrangedList") ?: JSONArray()
            for (i in 0 until arranged.length()) {
                val item = arranged.getJSONObject(i)
                val slot = slotFromEntry(item, weekly.week)
                val key = slotKey(slot)
                val existing = merged[key]
                if (existing == null) {
                    merged[key] = slot
                } else {
                    merged[key] = existing.copy(
                        selectedWeeks = (existing.selectedWeeks + weekly.week).distinct().sorted(),
                    )
                }
            }
        }
        return merged.values.toList()
    }

    /** 解析 `type=class` 单次返回的整学期课表（周次来自 `weeksAndTeachers`）。 */
    fun mergeClassResponse(body: String): List<ImportedCourseSlot> {
        val root = JSONObject(body)
        if (root.optString("code") != "0") {
            throw IllegalStateException(root.optString("msg", "课表请求失败"))
        }
        val arranged = root.optJSONObject("datas")?.optJSONArray("arrangedList") ?: JSONArray()
        val merged = linkedMapOf<String, ImportedCourseSlot>()
        for (i in 0 until arranged.length()) {
            val item = arranged.getJSONObject(i)
            val slot = slotFromClassEntry(item)
            val key = slotKey(slot)
            val existing = merged[key]
            if (existing == null) {
                merged[key] = slot
            } else {
                merged[key] = existing.copy(
                    selectedWeeks = (existing.selectedWeeks + slot.selectedWeeks).distinct().sorted(),
                )
            }
        }
        return merged.values.toList()
    }

    fun toImportJson(slots: List<ImportedCourseSlot>): String {
        val courses = JSONArray()
        slots.forEach { slot ->
            courses.put(
                JSONObject().apply {
                    put("groupId", slot.groupId)
                    put("name", slot.name)
                    put("teacher", slot.teacher)
                    put("location", slot.location)
                    put("dayOfWeek", slot.dayOfWeek)
                    put("startSection", slot.startSection)
                    put("endSection", slot.endSection)
                    put("credits", slot.credits)
                    put("cardColor", slot.colorHex)
                    put("selectedWeeks", JSONArray(slot.selectedWeeks))
                },
            )
        }
        return JSONObject().put("courses", courses).toString()
    }

    /** @see BuaaTermSelection.toTermCode */
    fun guessTermCode(year: Int, month: Int): String {
        val (calendarYear, season) = when (month) {
            in 8..12 -> year to BuaaTermSeason.AUTUMN
            in 6..7 -> year to BuaaTermSeason.SUMMER
            in 2..5 -> year to BuaaTermSeason.SPRING
            else -> year to BuaaTermSeason.AUTUMN
        }
        return BuaaTermSelection.toTermCode(calendarYear, season)
    }

    internal fun slotFromEntry(item: JSONObject, week: Int): ImportedCourseSlot {
        val teachClassId = item.optString("teachClassId").takeIf { it.isNotBlank() }
        val courseCode = item.optString("courseCode").takeIf { it.isNotBlank() }
        val serial = item.optString("courseSerialNo").takeIf { it.isNotBlank() }
        val groupId = teachClassId
            ?: listOfNotNull(courseCode, serial).joinToString("-").ifBlank { UUID.randomUUID().toString() }
        return ImportedCourseSlot(
            groupId = groupId,
            name = item.optString("courseName").ifBlank { "未命名课程" },
            teacher = parseTeacher(item.optString("weeksAndTeachers")),
            location = item.optString("placeName").takeIf { it.isNotBlank() },
            dayOfWeek = item.optInt("dayOfWeek"),
            startSection = item.optInt("beginSection"),
            endSection = item.optInt("endSection").takeIf { it > 0 }
                ?: item.optInt("beginSection"),
            credits = item.optString("credit").takeIf { it.isNotBlank() },
            colorHex = item.optString("color").ifBlank { "#E8D5D5" },
            selectedWeeks = listOf(week),
        )
    }

    internal fun slotFromClassEntry(item: JSONObject): ImportedCourseSlot {
        val teachClassId = item.optString("teachClassId").takeIf { it.isNotBlank() }
        val courseCode = item.optString("courseCode").takeIf { it.isNotBlank() }
        val serial = item.optString("courseSerialNo").takeIf { it.isNotBlank() }
        val groupId = teachClassId
            ?: listOfNotNull(courseCode, serial).joinToString("-").ifBlank { UUID.randomUUID().toString() }
        val weeksAndTeachers = item.optString("weeksAndTeachers")
        return ImportedCourseSlot(
            groupId = groupId,
            name = item.optString("courseName").ifBlank { "未命名课程" },
            teacher = parseTeacher(weeksAndTeachers),
            location = item.optString("placeName").takeIf { it.isNotBlank() },
            dayOfWeek = item.optInt("dayOfWeek"),
            startSection = item.optInt("beginSection"),
            endSection = item.optInt("endSection").takeIf { it > 0 }
                ?: item.optInt("beginSection"),
            credits = item.optString("credit").takeIf { it.isNotBlank() },
            colorHex = item.optString("color").ifBlank { "#E8D5D5" },
            selectedWeeks = parseSelectedWeeksFromWeeksAndTeachers(weeksAndTeachers),
        )
    }

    /** 从 `weeksAndTeachers`（如 `1-16周[理论]/…`）解析上课周次列表。 */
    fun parseSelectedWeeksFromWeeksAndTeachers(weeksAndTeachers: String?): List<Int> {
        if (weeksAndTeachers.isNullOrBlank()) return emptyList()
        val weekPart = weeksAndTeachers.substringBefore("/").substringBefore("[")
        val weeks = linkedSetOf<Int>()
        WEEK_RANGE_PATTERN.findAll(weekPart).forEach { match ->
            val start = match.groupValues[1].toIntOrNull() ?: return@forEach
            val end = match.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }?.toIntOrNull() ?: start
            for (week in start..end) {
                weeks.add(week)
            }
        }
        return weeks.sorted()
    }

    /** 从课表条目 `weeksAndTeachers`（如 `1-16周[理论]/…`）推断本学期最大教学周。 */
    fun inferMaxWeeksFromResponse(body: String): Int? {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return null
        val arranged = root.optJSONObject("datas")?.optJSONArray("arrangedList") ?: return null
        var maxWeek = 0
        for (i in 0 until arranged.length()) {
            val weeksAndTeachers = arranged.getJSONObject(i).optString("weeksAndTeachers")
            maxWeek = maxOf(maxWeek, parseMaxWeekFromWeeksAndTeachers(weeksAndTeachers))
        }
        return maxWeek.takeIf { it > 0 }
    }

    internal fun parseMaxWeekFromWeeksAndTeachers(weeksAndTeachers: String?): Int {
        if (weeksAndTeachers.isNullOrBlank()) return 0
        val weekPart = weeksAndTeachers.substringBefore("/").substringBefore("[")
        var maxWeek = 0
        WEEK_RANGE_PATTERN.findAll(weekPart).forEach { match ->
            val start = match.groupValues[1].toIntOrNull() ?: return@forEach
            val end = match.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }?.toIntOrNull() ?: start
            maxWeek = maxOf(maxWeek, start, end)
        }
        return maxWeek
    }

    private val WEEK_RANGE_PATTERN = Regex("(\\d+)(?:-(\\d+))?")

    internal fun parseTeacher(weeksAndTeachers: String?): String? {
        if (weeksAndTeachers.isNullOrBlank()) return null
        weeksAndTeachers.split("/").forEach { segment ->
            if (segment.contains("[主讲]")) {
                return segment.substringBefore("[主讲]").trim().ifBlank { null }
            }
        }
        return weeksAndTeachers.substringBefore("[").trim().removePrefix("周").trim().ifBlank { null }
    }

    internal fun slotKey(slot: ImportedCourseSlot): String =
        "${slot.groupId}|${slot.dayOfWeek}|${slot.startSection}|${slot.endSection}"
}

data class WeeklyResponse(
    val week: Int,
    val body: String,
)

data class ImportedCourseSlot(
    val groupId: String,
    val name: String,
    val teacher: String?,
    val location: String?,
    val dayOfWeek: Int,
    val startSection: Int,
    val endSection: Int,
    val credits: String?,
    val colorHex: String,
    val selectedWeeks: List<Int>,
)
