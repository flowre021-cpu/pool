package com.example.pool.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseIntegrityTest {
    private lateinit var database: AppDatabase

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
        ).build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun replaceCourseGroup_rollsBackDelete_whenInsertFails() = runBlocking {
        val dao = database.courseDao()
        dao.insert(
            course(id = 1, groupId = "group", name = "原课程"),
        )

        try {
            dao.replaceGroup(
                "group",
                listOf(
                    course(id = 2, groupId = "group", name = "新课程一"),
                    course(id = 2, groupId = "group", name = "新课程二"),
                ),
            )
            throw AssertionError("重复主键应导致插入失败")
        } catch (_: android.database.sqlite.SQLiteConstraintException) {
            // Expected: @Transaction must restore the deleted row.
        }

        val stored = dao.getByGroupId("group")
        assertEquals(1, stored.size)
        assertEquals("原课程", stored.single().name)
    }

    @Test
    fun semesterOperations_neverRemoveTheOnlyActiveSemester() = runBlocking {
        val dao = database.semesterDao()
        val active = SemesterEntity(
            id = "spring",
            name = "春季学期",
            startDateEpochDay = 1,
            endDateEpochDay = 126,
            isActive = true,
        )
        dao.insert(active)

        dao.upsertKeepingActive(active.copy(isActive = false))
        assertTrue(dao.getById("spring")!!.isActive)

        dao.setActive("missing")
        assertTrue(dao.getById("spring")!!.isActive)
        assertEquals(1, dao.countActive())
    }

    private fun course(
        id: Long,
        groupId: String,
        name: String,
    ) = CourseEntity(
        id = id,
        groupId = groupId,
        name = name,
        cardColor = 0xFFFFFFFF,
        selectedWeeks = listOf(1),
        dayOfWeek = 1,
        startSection = 1,
        endSection = 2,
    )
}
