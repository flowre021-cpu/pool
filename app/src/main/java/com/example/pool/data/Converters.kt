package com.example.pool.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromAffairType(type: AffairType): String = type.name

    @TypeConverter
    fun toAffairType(value: String): AffairType =
        AffairType.entries.firstOrNull { it.name == value } ?: AffairType.TASK

    @TypeConverter
    fun fromWeekList(weeks: List<Int>): String = weeks.joinToString(",")

    @TypeConverter
    fun toWeekList(value: String): List<Int> =
        if (value.isBlank()) emptyList()
        else value.split(",").mapNotNull { it.trim().toIntOrNull() }
}
