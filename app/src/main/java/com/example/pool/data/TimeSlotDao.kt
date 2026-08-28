package com.example.pool.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeSlotDao {
    @Query("SELECT * FROM time_slots ORDER BY sectionNumber")
    fun getAll(): Flow<List<TimeSlotEntity>>

    @Query("SELECT * FROM time_slots ORDER BY sectionNumber")
    suspend fun getAllOnce(): List<TimeSlotEntity>

    @Query("SELECT * FROM time_slots WHERE sectionNumber = :section")
    suspend fun getBySection(section: Int): TimeSlotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(slots: List<TimeSlotEntity>)

    @Update
    suspend fun update(slot: TimeSlotEntity)

    @Transaction
    suspend fun seedIfEmpty(slots: List<TimeSlotEntity>) {
        if (countAll() == 0) {
            insertAll(slots)
        }
    }

    @Query("SELECT COUNT(*) FROM time_slots")
    suspend fun countAll(): Int
}
