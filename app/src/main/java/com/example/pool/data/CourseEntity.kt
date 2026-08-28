package com.example.pool.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "courses",
    indices = [Index(value = ["dayOfWeek"])],
)
data class CourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: String,
    val name: String,
    val teacher: String? = null,
    val credits: String? = null,
    val note: String? = null,
    val cardColor: Long,
    /** 实际上课周次列表，如 [1,2,3,...,20]；未包含的周为免修/不上课 */
    val selectedWeeks: List<Int>,
    val dayOfWeek: Int,
    val startSection: Int,
    val endSection: Int,
    val location: String? = null,
    val semesterId: String? = null,
)
