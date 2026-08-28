package com.example.pool.ui.task

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pool.data.AffairEntity
import com.example.pool.data.AffairType
import com.example.pool.data.PlannerRepository
import com.example.pool.widget.WidgetRefreshScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(
    private val repository: PlannerRepository,
    private val appContext: Context,
) : ViewModel() {
    val tasks = repository.getTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun getTask(id: Long): AffairEntity? = repository.getTaskById(id)

    fun toggleDone(task: AffairEntity) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isDone = !task.isDone))
            refreshDdlWidget()
        }
    }

    fun deleteTask(task: AffairEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
            refreshDdlWidget()
        }
    }

    fun deleteTasks(tasks: Collection<AffairEntity>) {
        viewModelScope.launch {
            tasks.forEach { repository.deleteTask(it) }
            refreshDdlWidget()
        }
    }

    fun saveTask(
        id: Long,
        title: String,
        deadline: Long?,
        note: String?,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            if (id == 0L) {
                repository.insertTask(
                    AffairEntity(
                        type = AffairType.TASK,
                        title = title,
                        deadline = deadline,
                        note = note,
                    ),
                )
            } else {
                val existing = repository.getTaskById(id) ?: return@launch
                repository.updateTask(
                    existing.copy(
                        title = title,
                        deadline = deadline,
                        note = note,
                    ),
                )
            }
            refreshDdlWidget()
            onSaved()
        }
    }

    private fun refreshDdlWidget() {
        WidgetRefreshScheduler.requestRefreshForAffairType(appContext, AffairType.TASK)
    }
}

class TaskViewModelFactory(
    private val repository: PlannerRepository,
    private val appContext: Context,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TaskViewModel(repository, appContext) as T
    }
}
