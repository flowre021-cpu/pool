package com.example.pool.data.schedule.import

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuaaZhengfangParserTest {
    private val sampleBody = """
        {
            "datas": {
                "arrangedList": [
                    {
                        "week": null,
                        "beginTime": "08:00",
                        "endTime": "09:35",
                        "courseCode": "B310023002",
                        "beginSection": 1,
                        "endSection": 2,
                        "courseSerialNo": "103",
                        "weeksAndTeachers": "1-16周[理论]/张老师[主讲]",
                        "placeName": "田径场跑廊",
                        "teachClassId": "TC001",
                        "courseName": "体育（2）",
                        "credit": "0.5",
                        "color": "#FFF0CC",
                        "dayOfWeek": 5
                    }
                ],
                "practiceList": [],
                "code": "123456",
                "name": "测试[123456]"
            },
            "code": "0",
            "msg": null
        }
    """.trimIndent()

    @Test
    fun mergeWeeklyResponses_combinesWeeksForSameSlot() {
        val merged = BuaaZhengfangParser.mergeWeeklyResponses(
            listOf(
                WeeklyResponse(6, sampleBody),
                WeeklyResponse(7, sampleBody),
            ),
        )
        assertEquals(1, merged.size)
        assertEquals(listOf(6, 7), merged.single().selectedWeeks)
        assertEquals("体育（2）", merged.single().name)
        assertEquals(5, merged.single().dayOfWeek)
        assertEquals("张老师", merged.single().teacher)
    }

    @Test
    fun toImportJson_containsPoolFields() {
        val json = BuaaZhengfangParser.toImportJson(
            listOf(
                ImportedCourseSlot(
                    groupId = "TC001",
                    name = "体育（2）",
                    teacher = "张老师",
                    location = "田径场跑廊",
                    dayOfWeek = 5,
                    startSection = 1,
                    endSection = 2,
                    credits = "0.5",
                    colorHex = "#FFF0CC",
                    selectedWeeks = listOf(1, 2, 3),
                ),
            ),
        )
        val root = org.json.JSONObject(json)
        val course = root.getJSONArray("courses").getJSONObject(0)
        assertEquals("TC001", course.getString("groupId"))
        assertEquals(1, course.getInt("startSection"))
        assertEquals(3, course.getJSONArray("selectedWeeks").length())
    }

    @Test
    fun inferMaxWeeksFromResponse_readsWeeksAndTeachers() {
        assertEquals(16, BuaaZhengfangParser.inferMaxWeeksFromResponse(sampleBody))
    }

    @Test
    fun parseMaxWeekFromWeeksAndTeachers_parsesRange() {
        assertEquals(16, BuaaZhengfangParser.parseMaxWeekFromWeeksAndTeachers("1-16周[理论]/张老师[主讲]"))
    }

    @Test
    fun parseMaxWeekFromWeeksAndTeachers_parsesSingleWeek() {
        assertEquals(17, BuaaZhengfangParser.parseMaxWeekFromWeeksAndTeachers("17周[实验]"))
    }

    @Test
    fun mergeClassResponse_parsesWeekRangesFromWeeksAndTeachers() {
        val merged = BuaaZhengfangParser.mergeClassResponse(sampleBody)
        assertEquals(1, merged.size)
        assertEquals((1..16).toList(), merged.single().selectedWeeks)
    }

    @Test
    fun parseSelectedWeeksFromWeeksAndTeachers_parsesDisjointWeeks() {
        assertEquals(
            listOf(1, 2, 3, 8, 9),
            BuaaZhengfangParser.parseSelectedWeeksFromWeeksAndTeachers("1-3,8-9周[理论]"),
        )
    }

    @Test
    fun guessTermCode_springSemester() {
        assertEquals("2025-2026-2", BuaaZhengfangParser.guessTermCode(2026, 3))
    }

    @Test
    fun guessTermCode_summerSemester() {
        assertEquals("2025-2026-3", BuaaZhengfangParser.guessTermCode(2026, 7))
    }

    @Test
    fun guessTermCode_fallSemester() {
        assertEquals("2026-2027-1", BuaaZhengfangParser.guessTermCode(2026, 9))
    }
}
