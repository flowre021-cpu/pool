package com.example.pool.data

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

@Entity(tableName = "semesters")
data class SemesterEntity(
    @PrimaryKey val id: String,
    val name: String,
    /** LocalDate.toEpochDay() */
    val startDateEpochDay: Long,
    /** LocalDate.toEpochDay()，首尾日期均包含在学期内 */
    @ColumnInfo(defaultValue = "0")
    val endDateEpochDay: Long,
    val isActive: Boolean = false,
)
