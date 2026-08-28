package com.example.pool.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pool.ui.course.model.SchedulePastelColors
import com.example.pool.util.formatBlockTimeRange
import com.example.pool.util.formatEventLocation
import com.example.pool.util.formatMinutesOfDayCompact

@Composable
fun AgendaBlockCard(
    block: AgendaGridBlock,
    density: AgendaCardDensity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    useScheduleStyle: Boolean = false,
    showTimeRange: Boolean = true,
    cardWidth: Dp = Dp.Unspecified,
    cardHeight: Dp = Dp.Unspecified,
    zoomLevel: AgendaZoomLevel = AgendaZoomLevel.WEEK,
    simplifyContent: Boolean = false,
    interactionEnabled: Boolean = true,
) {
    val color = when (block) {
        is AgendaGridBlock.Course -> block.color
        is AgendaGridBlock.Event -> block.color
    }
    val primaryText = SchedulePastelColors.textOnCard(color)
    val secondaryText = SchedulePastelColors.textOnCard(color, secondary = true)
    val accessibilityLabel = when (block) {
        is AgendaGridBlock.Course -> "课程：${block.name}"
        is AgendaGridBlock.Event -> "事件：${block.title}"
    }

    Surface(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                contentDescription = accessibilityLabel
                role = Role.Button
            }
            .clickable(enabled = interactionEnabled, onClick = onClick),
        color = color,
        shape = RoundedCornerShape(6.dp),
    ) {
        if (simplifyContent) {
            MorphSimplifiedCardContent(
                block = block,
                primaryText = primaryText,
            )
        } else {
            val timeLayout = if (showTimeRange && cardWidth != Dp.Unspecified && cardHeight != Dp.Unspecified) {
                resolveEventTimeLayout(cardWidth, cardHeight, zoomLevel)
            } else if (showTimeRange) {
                EventTimeLayout.TWO_LINE
            } else {
                EventTimeLayout.HIDDEN
            }
            when {
                useScheduleStyle -> ScheduleStyleCardContent(
                    block = block,
                    primaryText = primaryText,
                    secondaryText = secondaryText,
                    timeLayout = timeLayout,
                    cardHeight = cardHeight,
                )
                density == AgendaCardDensity.MINIMAL -> OverlapMinimalCardContent(
                    block = block,
                    primaryText = primaryText,
                    secondaryText = secondaryText,
                    timeLayout = timeLayout,
                )
                density == AgendaCardDensity.COMPACT -> CompactCardContent(
                    block = block,
                    primaryText = primaryText,
                    secondaryText = secondaryText,
                    timeLayout = timeLayout,
                )
                else -> FullCardContent(
                    block = block,
                    primaryText = primaryText,
                    secondaryText = secondaryText,
                    timeLayout = timeLayout,
                    cardHeight = cardHeight,
                )
            }
        }
    }
}

/** 视图切换动画期间：仅保留标题，降低测量/绘制开销 */
@Composable
private fun MorphSimplifiedCardContent(
    block: AgendaGridBlock,
    primaryText: Color,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 3.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = blockTitle(block),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                lineHeight = 12.sp,
            ),
            fontWeight = FontWeight.SemiBold,
            color = primaryText,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun OverlapMinimalCardContent(
    block: AgendaGridBlock,
    primaryText: Color,
    secondaryText: Color,
    timeLayout: EventTimeLayout,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = blockTitle(block),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                lineHeight = 11.sp,
            ),
            fontWeight = FontWeight.SemiBold,
            color = primaryText,
            textAlign = TextAlign.Center,
            maxLines = if (timeLayout != EventTimeLayout.HIDDEN) 2 else 4,
            overflow = TextOverflow.Ellipsis,
        )
        if (block is AgendaGridBlock.Event && timeLayout != EventTimeLayout.HIDDEN) {
            EventTimeText(
                block = block,
                color = secondaryText,
                layout = timeLayout,
                compact = true,
            )
        }
    }
}

@Composable
private fun ScheduleStyleCardContent(
    block: AgendaGridBlock,
    primaryText: Color,
    secondaryText: Color,
    timeLayout: EventTimeLayout,
    cardHeight: Dp,
) {
    val titleMaxLines = when {
        block !is AgendaGridBlock.Event || timeLayout == EventTimeLayout.HIDDEN -> 6
        cardHeight != Dp.Unspecified && cardHeight < 52.dp -> 1
        cardHeight != Dp.Unspecified && cardHeight < 72.dp -> 2
        else -> 4
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 6.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = blockTitle(block),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = primaryText,
            textAlign = TextAlign.Center,
            maxLines = titleMaxLines,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 14.sp,
        )
        when (block) {
            is AgendaGridBlock.Course -> {
                if (!block.location.isNullOrBlank()) {
                    Text(
                        text = "@${block.location}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
                        color = secondaryText,
                        textAlign = TextAlign.Center,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!block.teacher.isNullOrBlank()) {
                    Text(
                        text = block.teacher,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
                        color = secondaryText,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!block.note.isNullOrBlank()) {
                    Text(
                        text = block.note,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
                        color = secondaryText,
                        textAlign = TextAlign.Center,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            is AgendaGridBlock.Event -> {
                if (timeLayout != EventTimeLayout.HIDDEN) {
                    EventTimeText(
                        block = block,
                        color = secondaryText,
                        layout = timeLayout,
                    )
                }
                if (timeLayout != EventTimeLayout.STACKED || cardHeight == Dp.Unspecified || cardHeight >= 56.dp) {
                    Text(
                        text = formatEventLocation(block.location),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
                        color = secondaryText,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun EventTimeText(
    block: AgendaGridBlock,
    color: Color,
    layout: EventTimeLayout,
    compact: Boolean = false,
) {
    val startText = formatMinutesOfDayCompact(block.startMinutes)
    val endText = formatMinutesOfDayCompact(block.endMinutes)
    val singleLine = formatBlockTimeRange(block.startMinutes, block.endMinutes)
    val fontSize = if (compact) 9.sp else 10.sp
    val timeStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = fontSize,
        lineHeight = if (compact) 11.sp else 12.sp,
    )

    when (layout) {
        EventTimeLayout.HIDDEN -> Unit
        EventTimeLayout.START_ONLY -> {
            Text(
                text = startText,
                style = timeStyle,
                color = color,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
        EventTimeLayout.SINGLE_LINE -> {
            Text(
                text = singleLine,
                style = timeStyle,
                color = color,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
            )
        }
        EventTimeLayout.TWO_LINE -> {
            Text(
                text = "$startText-\n$endText",
                style = timeStyle,
                color = color,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Clip,
            )
        }
        EventTimeLayout.STACKED -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                Text(
                    text = startText,
                    style = timeStyle,
                    color = color,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
                Text(
                    text = endText,
                    style = timeStyle,
                    color = color,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun CompactCardContent(
    block: AgendaGridBlock,
    primaryText: Color,
    secondaryText: Color,
    timeLayout: EventTimeLayout,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = blockTitle(block),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = primaryText,
            maxLines = if (timeLayout != EventTimeLayout.HIDDEN) 2 else 3,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 12.sp,
            textAlign = TextAlign.Center,
        )
        if (block is AgendaGridBlock.Event && timeLayout != EventTimeLayout.HIDDEN) {
            EventTimeText(
                block = block,
                color = secondaryText,
                layout = timeLayout,
                compact = true,
            )
        }
    }
}

@Composable
private fun FullCardContent(
    block: AgendaGridBlock,
    primaryText: Color,
    secondaryText: Color,
    timeLayout: EventTimeLayout,
    cardHeight: Dp,
) {
    ScheduleStyleCardContent(
        block = block,
        primaryText = primaryText,
        secondaryText = secondaryText,
        timeLayout = timeLayout,
        cardHeight = cardHeight,
    )
}

private fun blockTitle(block: AgendaGridBlock): String = when (block) {
    is AgendaGridBlock.Course -> block.name
    is AgendaGridBlock.Event -> block.title
}
