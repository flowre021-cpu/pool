package com.example.pool.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

/** 事务（Task / Opportunity / Event / Reminder）数据仓库；课表请使用 ScheduleRepository */
class PlannerRepository(
    private val affairDao: AffairDao,
    appScope: CoroutineScope,
) {
    private val allAffairs = affairDao.observeAll()
        .shareIn(appScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    fun getByType(type: AffairType): Flow<List<AffairEntity>> =
        allAffairs
            .map { affairs -> affairs.filter { it.type == type } }
            .distinctUntilChanged()

    suspend fun getById(id: Long): AffairEntity? = affairDao.getById(id)

    suspend fun listByType(type: AffairType): List<AffairEntity> = affairDao.listByType(type)

    suspend fun insert(affair: AffairEntity): Long = affairDao.insert(affair)

    suspend fun update(affair: AffairEntity) = affairDao.update(affair)

    suspend fun delete(affair: AffairEntity) = affairDao.delete(affair)

    // Task 兼容别名
    fun getTasks(): Flow<List<AffairEntity>> = getByType(AffairType.TASK)

    suspend fun getTaskById(id: Long): AffairEntity? = getById(id)

    suspend fun insertTask(task: AffairEntity): Long = insert(task)

    suspend fun updateTask(task: AffairEntity) = update(task)

    suspend fun deleteTask(task: AffairEntity) = delete(task)
}
