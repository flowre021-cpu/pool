package com.example.pool.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import com.example.pool.PoolApplication

class TodayScheduleWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        WidgetTheme.sync(context)
        val app = context.applicationContext as PoolApplication
        val snapshot = WidgetDataLoader.load(app.repository, app.scheduleRepository)
        val items = WidgetFocus.reorderTodayItemsForFocus(snapshot.todayItems)
        provideContent {
            TodayScheduleWidgetContent(context = context, items = items)
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        provideContent {
            TodayScheduleWidgetContent(
                context = context,
                items = WidgetPreviewSamples.todayItems,
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun TodayScheduleWidgetContent(context: Context, items: List<WidgetScheduleItem>) {
    WidgetScaffold(context = context) {
        item {
            WidgetSectionTitle(context = context, text = "今日")
        }
        if (items.isEmpty()) {
            item {
                WidgetEmptyHint(context = context, text = "暂无安排")
            }
        } else {
            items(
                items = items,
                itemId = { item ->
                    when (item.kind) {
                        WidgetScheduleKind.COURSE -> item.id
                        WidgetScheduleKind.EVENT -> item.id + 1_000_000_000L
                    }
                },
            ) { item ->
                WidgetScheduleRow(context = context, item = item)
            }
        }
    }
}
