package com.example.pool.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "time_slots")
data class TimeSlotEntity(
    @PrimaryKey val sectionNumber: Int,
    val startTimeMinutes: Int,
    val endTimeMinutes: Int,
)
