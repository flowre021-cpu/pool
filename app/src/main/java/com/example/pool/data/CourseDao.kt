package com.example.pool.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses ORDER BY dayOfWeek, startSection")
    fun getAll(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses ORDER BY dayOfWeek, startSection")
    suspend fun getAllOnce(): List<CourseEntity>

    @Query("SELECT * FROM courses WHERE id = :id")
    suspend fun getById(id: Long): CourseEntity?

    @Query("SELECT * FROM courses WHERE groupId = :groupId ORDER BY dayOfWeek, startSection")
    suspend fun getByGroupId(groupId: String): List<CourseEntity>

    @Insert
    suspend fun insert(course: CourseEntity): Long

    @Insert
    suspend fun insertAll(courses: List<CourseEntity>)

    @Update
    suspend fun update(course: CourseEntity)

    @Delete
    suspend fun delete(course: CourseEntity)

    @Query("DELETE FROM courses WHERE groupId = :groupId")
    suspend fun deleteByGroupId(groupId: String)

    @Query("DELETE FROM courses WHERE semesterId = :semesterId")
    suspend fun deleteBySemesterId(semesterId: String)

    @Query("DELETE FROM courses")
    suspend fun deleteAll()

    @Query("UPDATE courses SET cardColor = :color WHERE groupId = :groupId")
    suspend fun updateCardColorForGroup(groupId: String, color: Long)

    @Transaction
    suspend fun replaceGroup(groupId: String, courses: List<CourseEntity>) {
        deleteByGroupId(groupId)
        insertAll(courses)
    }

    @Transaction
    suspend fun seedIfEmpty(courses: List<CourseEntity>) {
        if (countAll() == 0) {
            insertAll(courses)
        }
    }

    @Query("SELECT COUNT(*) FROM courses")
    suspend fun countAll(): Int
}
