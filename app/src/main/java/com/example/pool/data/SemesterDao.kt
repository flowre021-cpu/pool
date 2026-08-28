package com.example.pool.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SemesterDao {
    @Query("SELECT * FROM semesters ORDER BY startDateEpochDay DESC")
    suspend fun getAllOnce(): List<SemesterEntity>

    @Query("SELECT * FROM semesters ORDER BY startDateEpochDay DESC")
    fun getAll(): Flow<List<SemesterEntity>>

    @Query("SELECT * FROM semesters WHERE isActive = 1 LIMIT 1")
    fun getActive(): Flow<SemesterEntity?>

    @Query("SELECT * FROM semesters WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveOnce(): SemesterEntity?

    @Query("SELECT * FROM semesters WHERE id = :id")
    suspend fun getById(id: String): SemesterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(semester: SemesterEntity)

    @Query("UPDATE semesters SET isActive = 0")
    suspend fun clearActive()

    @Query("SELECT COUNT(*) FROM semesters WHERE isActive = 1")
    suspend fun countActive(): Int

    @Transaction
    suspend fun upsertKeepingActive(semester: SemesterEntity) {
        val existing = getById(semester.id)
        val shouldBeActive = semester.isActive ||
            (existing?.isActive == true && countActive() <= 1) ||
            countActive() == 0
        if (shouldBeActive) {
            clearActive()
        }
        insert(semester.copy(isActive = shouldBeActive))
    }

    @Transaction
    suspend fun seedIfEmpty(semesters: List<SemesterEntity>) {
        if (countAll() == 0) {
            semesters.forEach { insert(it) }
        }
    }

    @Transaction
    suspend fun setActive(semesterId: String) {
        val semester = getById(semesterId) ?: return
        clearActive()
        insert(semester.copy(isActive = true))
    }

    @Query("SELECT COUNT(*) FROM semesters")
    suspend fun countAll(): Int
}
