package com.example.pool.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import com.example.pool.PoolApplication

class DdlWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        WidgetTheme.sync(context)
        val app = context.applicationContext as PoolApplication
        val items = WidgetDataLoader.loadDeadlineItems(app.repository)
        provideContent {
            DdlWidgetContent(context = context, items = items)
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        provideContent {
            DdlWidgetContent(context = context, items = WidgetPreviewSamples.deadlineItems)
        }
    }
}

@Composable
private fun DdlWidgetContent(
    context: Context,
    items: List<WidgetDeadlineItem>,
) {
    val visibleItems = WidgetFocus.reorderDeadlineItemsForFocus(items)
    WidgetScaffold(context = context) {
        item {
            WidgetSectionTitle(context = context, text = "DDL")
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
                WidgetDeadlineRow(context = context, item = item)
            }
        }
    }
}
