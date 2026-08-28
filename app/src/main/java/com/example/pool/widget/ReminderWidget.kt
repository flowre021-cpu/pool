package com.example.pool.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import com.example.pool.PoolApplication

class ReminderWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        WidgetTheme.sync(context)
        val app = context.applicationContext as PoolApplication
        val items = WidgetDataLoader.loadReminderItems(app.repository)
        provideContent {
            ReminderWidgetContent(context = context, items = items)
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        provideContent {
            ReminderWidgetContent(context = context, items = WidgetPreviewSamples.reminderItems)
        }
    }
}

@Composable
private fun ReminderWidgetContent(
    context: Context,
    items: List<WidgetReminderItem>,
) {
    val visibleItems = WidgetFocus.reorderReminderItemsForFocus(items)
    WidgetScaffold(context = context) {
        item {
            WidgetSectionTitle(context = context, text = "提醒")
        }
        if (visibleItems.isEmpty()) {
            item {
                WidgetEmptyHint(context = context, text = "无")
            }
        } else {
            items(
                items = visibleItems,
                itemId = { item -> item.id },
            ) { item ->
                WidgetReminderRow(context = context, item = item)
            }
        }
    }
}
