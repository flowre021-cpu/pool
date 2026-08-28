package com.example.pool.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate6To7_preservesSemesterAndAddsEighteenWeekEndDate() {
        helper.createDatabase(TEST_DATABASE, 6).use { database ->
            database.execSQL(
                """
                INSERT INTO semesters (id, name, startDateEpochDay, isActive)
                VALUES ('spring', '春季学期', 1000, 1)
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            7,
            true,
            AppDatabase.MIGRATION_6_7,
        ).use { database ->
            database.query(
                "SELECT startDateEpochDay, endDateEpochDay FROM semesters WHERE id = 'spring'",
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals(1000L, cursor.getLong(0))
                assertEquals(1125L, cursor.getLong(1))
            }
        }
    }

    @Test
    fun migrate7To8_addsCompletedOccurrenceDaysColumn() {
        helper.createDatabase(TEST_DATABASE, 7).use { database ->
            database.execSQL(
                """
                INSERT INTO affairs (id, type, title, isDone, createdAt)
                VALUES (1, 'REMINDER', '每日提醒', 0, 100)
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            8,
            true,
            AppDatabase.MIGRATION_7_8,
        ).use { database ->
            database.query(
                "SELECT completedOccurrenceDays FROM affairs WHERE id = 1",
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals(null, cursor.getString(0))
            }
        }
    }

    @Test
    fun migrate8To9_addsTypeAndDayOfWeekIndexes() {
        helper.createDatabase(TEST_DATABASE, 8).use { database ->
            database.execSQL(
                """
                INSERT INTO affairs (id, type, title, isDone, createdAt)
                VALUES (1, 'TASK', '作业', 0, 100)
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO courses (
                    id, groupId, name, cardColor, selectedWeeks, dayOfWeek, startSection, endSection
                ) VALUES (1, 'g1', '数学', 1, '1,2', 1, 1, 2)
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            9,
            true,
            AppDatabase.MIGRATION_8_9,
        ).use { database ->
            database.query("PRAGMA index_list(affairs)").use { cursor ->
                val indexes = mutableListOf<String>()
                while (cursor.moveToNext()) {
                    indexes += cursor.getString(1)
                }
                assertEquals(true, indexes.contains("index_affairs_type"))
            }
            database.query("PRAGMA index_list(courses)").use { cursor ->
                val indexes = mutableListOf<String>()
                while (cursor.moveToNext()) {
                    indexes += cursor.getString(1)
                }
                assertEquals(true, indexes.contains("index_courses_dayOfWeek"))
            }
        }
    }

    @Test
    fun migrate9To10_addsCourseSemesterIdColumn() {
        helper.createDatabase(TEST_DATABASE, 9).use { database ->
            database.execSQL(
                """
                INSERT INTO courses (
                    id, groupId, name, cardColor, selectedWeeks, dayOfWeek, startSection, endSection
                ) VALUES (1, 'g1', '数学', 1, '1,2', 1, 1, 2)
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            10,
            true,
            AppDatabase.MIGRATION_9_10,
        ).use { database ->
            database.query("SELECT semesterId FROM courses WHERE id = 1").use { cursor ->
                cursor.moveToFirst()
                assertEquals(null, cursor.getString(0))
            }
        }
    }

    private companion object {
        const val TEST_DATABASE = "migration-test"
    }
}
