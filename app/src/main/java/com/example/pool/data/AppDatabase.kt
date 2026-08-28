package com.example.pool.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AffairEntity::class,
        CourseEntity::class,
        TimeSlotEntity::class,
        SemesterEntity::class,
    ],
    version = 10,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun affairDao(): AffairDao

    abstract fun courseDao(): CourseDao

    abstract fun timeSlotDao(): TimeSlotDao

    abstract fun semesterDao(): SemesterDao

    companion object {
        /** 旧学期没有结束日期，迁移时按常见的 18 个教学周补齐。 */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE semesters ADD COLUMN endDateEpochDay INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "UPDATE semesters SET endDateEpochDay = startDateEpochDay + 125 " +
                        "WHERE endDateEpochDay = 0",
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE affairs ADD COLUMN completedOccurrenceDays TEXT",
                )
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_affairs_type ON affairs(type)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_courses_dayOfWeek ON courses(dayOfWeek)")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE courses ADD COLUMN semesterId TEXT")
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pool.db",
                )
                    .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
