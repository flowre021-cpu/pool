package com.example.pool.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.example.pool.PoolApplication
import com.example.pool.data.AffairType
import com.example.pool.data.PlannerRepository
import com.example.pool.util.toggleAffairDone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ToggleDoneActionKeys {
    val AffairId = ActionParameters.Key<Long>("affair_id")
    val AffairType = ActionParameters.Key<String>("affair_type")
}

class ToggleDoneAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val affairId = parameters[ToggleDoneActionKeys.AffairId] ?: return
        val affairType = parameters[ToggleDoneActionKeys.AffairType]
            ?.let { name -> AffairType.entries.firstOrNull { it.name == name } }
            ?: return
        val app = context.applicationContext as PoolApplication
        toggleDone(app.repository, affairId)
        WidgetImmediateUpdater.kindFor(affairType)?.let { kind ->
            WidgetImmediateUpdater.pushUi(context, glanceId, kind)
        }
    }

    private suspend fun toggleDone(repository: PlannerRepository, affairId: Long) {
        withContext(Dispatchers.IO) {
            val affair = repository.getById(affairId) ?: return@withContext
            repository.update(toggleAffairDone(affair))
        }
    }
}
