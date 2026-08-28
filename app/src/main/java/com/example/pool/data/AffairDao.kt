package com.example.pool.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AffairDao {
    @Query("SELECT * FROM affairs ORDER BY type, createdAt DESC")
    fun observeAll(): Flow<List<AffairEntity>>

    @Query(
        """
        SELECT * FROM affairs WHERE type = :type
        ORDER BY
            COALESCE(deadline, startAt, endAt, createdAt) IS NULL,
            COALESCE(deadline, startAt, endAt, createdAt) ASC,
            createdAt DESC
        """,
    )
    fun getByType(type: AffairType): Flow<List<AffairEntity>>

    @Query(
        """
        SELECT * FROM affairs WHERE type = :type
        ORDER BY
            COALESCE(deadline, startAt, endAt, createdAt) IS NULL,
            COALESCE(deadline, startAt, endAt, createdAt) ASC,
            createdAt DESC
        """,
    )
    suspend fun listByType(type: AffairType): List<AffairEntity>

    @Query("SELECT * FROM affairs WHERE id = :id")
    suspend fun getById(id: Long): AffairEntity?

    @Insert
    suspend fun insert(affair: AffairEntity): Long

    @Update
    suspend fun update(affair: AffairEntity)

    @Delete
    suspend fun delete(affair: AffairEntity)

    @Query("SELECT COUNT(*) FROM affairs")
    suspend fun countAll(): Int
}
