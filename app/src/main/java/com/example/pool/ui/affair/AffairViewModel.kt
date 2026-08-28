package com.example.pool.ui.affair

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
import com.example.pool.util.toggleAffairDone
import kotlinx.coroutines.launch

class AffairViewModel(
    private val repository: PlannerRepository,
    private val appContext: Context,
    val affairType: AffairType,
) : ViewModel() {
    val affairs = repository.getByType(affairType)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun getAffair(id: Long): AffairEntity? = repository.getById(id)

    fun toggleDone(affair: AffairEntity) {
        viewModelScope.launch {
            repository.update(toggleAffairDone(affair))
            refreshWidgetIfNeeded()
        }
    }

    fun deleteAffair(affair: AffairEntity) {
        viewModelScope.launch {
            repository.delete(affair)
            refreshWidgetIfNeeded()
        }
    }

    fun deleteAffairs(affairs: Collection<AffairEntity>) {
        viewModelScope.launch {
            affairs.forEach { repository.delete(it) }
            refreshWidgetIfNeeded()
        }
    }

    fun saveAffair(affair: AffairEntity, onSaved: () -> Unit) {
        viewModelScope.launch {
            if (affair.id == 0L) {
                repository.insert(affair)
            } else {
                val existing = repository.getById(affair.id) ?: return@launch
                repository.update(mergeAffairEdit(existing, affair))
            }
            refreshWidgetIfNeeded()
            onSaved()
        }
    }

    private fun refreshWidgetIfNeeded() {
        WidgetRefreshScheduler.requestRefreshForAffairType(appContext, affairType)
    }
}

internal fun mergeAffairEdit(
    existing: AffairEntity,
    edited: AffairEntity,
): AffairEntity {
    val common = existing.copy(
        title = edited.title,
        note = edited.note,
    )
    return when (existing.type) {
        AffairType.TASK -> common.copy(deadline = edited.deadline)
        AffairType.REMINDER -> common.copy(
            startAt = edited.startAt,
            recurrenceRule = edited.recurrenceRule,
        )
        AffairType.OPPORTUNITY -> common.copy(
            startAt = edited.startAt,
            endAt = edited.endAt,
            cardColor = edited.cardColor,
        )
        AffairType.EVENT -> common.copy(
            startAt = edited.startAt,
            endAt = edited.endAt,
            location = edited.location,
            cardColor = edited.cardColor,
        )
    }
}

class AffairViewModelFactory(
    private val repository: PlannerRepository,
    private val appContext: Context,
    private val affairType: AffairType,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AffairViewModel(repository, appContext, affairType) as T
    }
}
