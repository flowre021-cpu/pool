package com.example.pool.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.LazyListScope
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.unit.FixedColorProvider
import com.example.pool.data.AffairType

import com.example.pool.ui.theme.PoolColors
import com.example.pool.ui.theme.PoolFixedColors

internal object WidgetColors {
    val Background get() = widgetColor(PoolColors.Block)
    val SectionTitle get() = widgetColor(PoolColors.AccentPrimary)
    val TextPrimary get() = widgetColor(PoolFixedColors.textPrimary)
    val TextSecondary get() = widgetColor(PoolFixedColors.textSecondary)
    val Divider get() = widgetColor(PoolFixedColors.divider)
    val CompleteAction get() = widgetColor(PoolColors.AccentPrimary)
}

internal fun widgetColor(color: Color): ColorProvider = FixedColorProvider(color)

internal fun widgetColor(argb: Long): ColorProvider =
    widgetColor(Color((argb and 0xFFFFFFFFL).toInt()))

internal fun GlanceModifier.widgetChrome(): GlanceModifier = this
    .fillMaxSize()
    .cornerRadius(16.dp)
    .background(WidgetColors.Background)

internal fun GlanceModifier.widgetOpenHome(context: Context): GlanceModifier =
    clickable(WidgetNavigation.openHomeAction(context))

@Composable
private fun WidgetCompleteActionButton(
    affairId: Long,
    affairType: AffairType,
) {
    Box(
        modifier = GlanceModifier
            .clickable(
                actionRunCallback<ToggleDoneAction>(
                    actionParametersOf(
                        ToggleDoneActionKeys.AffairId to affairId,
                        ToggleDoneActionKeys.AffairType to affairType.name,
                    ),
                ),
            )
            .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "○",
            style = TextStyle(
                color = WidgetColors.CompleteAction,
                fontSize = 16.sp,
            ),
        )
    }
}

/**
 * 外层 Box 负责空白区进首页；LazyColumn 仅 fillMaxWidth，可滚动且不占满高度，避免吞掉空白点击。
 */
@Composable
internal fun WidgetScaffold(
    context: Context,
    content: LazyListScope.() -> Unit,
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .widgetChrome()
            .widgetOpenHome(context),
    ) {
        LazyColumn(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(12.dp),
            content = content,
        )
    }
}

@Composable
internal fun WidgetSectionTitle(context: Context, text: String) {
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
            .widgetOpenHome(context),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            style = TextStyle(
                color = WidgetColors.SectionTitle,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            ),
        )
    }
}

@Composable
internal fun WidgetEmptyHint(context: Context, text: String) {
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(bottom = 2.dp)
            .widgetOpenHome(context),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            style = TextStyle(
                color = WidgetColors.TextSecondary,
                fontSize = 12.sp,
            ),
        )
    }
}

@Composable
internal fun WidgetScheduleRow(context: Context, item: WidgetScheduleItem) {
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable(WidgetNavigation.openScheduleItemAction(context, item)),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Box(
                modifier = GlanceModifier
                    .width(4.dp)
                    .height(34.dp)
                    .cornerRadius(2.dp)
                    .background(widgetColor(item.colorArgb)),
            ) {}
            Spacer(GlanceModifier.width(6.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = formatMinutesRange(item.startMinutes, item.endMinutes),
                    style = TextStyle(
                        color = WidgetColors.TextSecondary,
                        fontSize = 11.sp,
                    ),
                )
                Text(
                    text = buildScheduleTitle(item),
                    style = TextStyle(
                        color = WidgetColors.TextPrimary,
                        fontSize = 12.sp,
                    ),
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
internal fun WidgetDeadlineRow(context: Context, item: WidgetDeadlineItem) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .defaultWeight()
                .clickable(WidgetNavigation.openAffairListAction(context, AffairType.TASK)),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = item.title,
                style = TextStyle(
                    color = WidgetColors.TextPrimary,
                    fontSize = 12.sp,
                ),
                maxLines = 1,
            )
        }
        Text(
            text = item.remainingLabel,
            style = TextStyle(
                color = WidgetColors.TextSecondary,
                fontSize = 11.sp,
            ),
            modifier = GlanceModifier.padding(end = 4.dp),
        )
        WidgetCompleteActionButton(
            affairId = item.id,
            affairType = AffairType.TASK,
        )
    }
}

@Composable
internal fun WidgetReminderRow(context: Context, item: WidgetReminderItem) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .defaultWeight()
                .clickable(WidgetNavigation.openAffairListAction(context, AffairType.REMINDER)),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = item.title,
                style = TextStyle(
                    color = WidgetColors.TextPrimary,
                    fontSize = 12.sp,
                ),
                maxLines = 1,
            )
        }
        WidgetCompleteActionButton(
            affairId = item.id,
            affairType = AffairType.REMINDER,
        )
    }
}

private fun buildScheduleTitle(item: WidgetScheduleItem): String =
    item.location?.let { "${item.title} · $it" } ?: item.title
