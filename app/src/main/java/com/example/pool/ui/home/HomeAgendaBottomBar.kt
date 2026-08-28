package com.example.pool.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pool.ui.theme.PoolColors

@Composable
fun HomeTimelineBottomBar(
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = PoolColors.Divider, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .navigationBarsPadding()
                .padding(vertical = 10.dp)
                .minimumInteractiveComponentSize()
                .clickable(onClick = onEditClick),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "编辑",
                tint = PoolColors.AccentPrimary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "编辑",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = PoolColors.AccentPrimary,
            )
        }
    }
}

@Composable
fun HomeAgendaBottomBar(
    zoomLevel: AgendaZoomLevel,
    timeLayoutMode: AgendaTimeLayoutMode,
    onEditClick: () -> Unit,
    onToggleZoomLevel: () -> Unit,
    onToggleTimeLayout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = PoolColors.Divider, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .navigationBarsPadding()
                .padding(top = 10.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeBottomBarAction(
                label = zoomLevel.contentDescription(),
                onClick = onToggleZoomLevel,
            ) {
                AnimatedContent(
                    targetState = zoomLevel,
                    transitionSpec = {
                        (fadeIn(tween(180)) + scaleIn(initialScale = 0.85f, animationSpec = tween(180)))
                            .togetherWith(
                                fadeOut(tween(140)) + scaleOut(targetScale = 0.85f, animationSpec = tween(140)),
                            )
                    },
                    label = "viewModeIcon",
                ) { level ->
                    AgendaViewModeIcon(level = level)
                }
            }
            HomeBottomBarAction(
                label = timeLayoutMode.contentDescription(),
                onClick = onToggleTimeLayout,
            ) {
                AnimatedContent(
                    targetState = timeLayoutMode,
                    transitionSpec = {
                        (fadeIn(tween(180)) + scaleIn(initialScale = 0.85f, animationSpec = tween(180)))
                            .togetherWith(
                                fadeOut(tween(140)) + scaleOut(targetScale = 0.85f, animationSpec = tween(140)),
                            )
                    },
                    label = "timeLayoutIcon",
                ) { mode ->
                    AgendaTimeLayoutIcon(mode = mode)
                }
            }
            HomeBottomBarAction(
                label = "编辑",
                onClick = onEditClick,
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "编辑",
                    tint = PoolColors.AccentPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun HomeBottomBarAction(
    label: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        icon()
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = PoolColors.TextSecondary,
        )
    }
}

@Composable
fun AgendaTimeLayoutIcon(
    mode: AgendaTimeLayoutMode,
    modifier: Modifier = Modifier,
    color: Color = PoolColors.AccentPrimary,
) {
    Box(modifier = modifier.size(22.dp), contentAlignment = Alignment.Center) {
        when (mode) {
            AgendaTimeLayoutMode.HOUR_24 -> TimeLayoutHourGridIcon(color = color, size = 16.dp)
            AgendaTimeLayoutMode.SECTION_TIMELINE -> TimeLayoutSectionIcon(color = color, size = 16.dp)
        }
    }
}

@Composable
private fun TimeLayoutHourGridIcon(color: Color, size: androidx.compose.ui.unit.Dp) {
    Row(modifier = Modifier.size(size)) {
        Column(
            modifier = Modifier
                .width(4.dp)
                .height(size),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(1.dp)
                        .background(color),
                )
            }
        }
        Column(
            modifier = Modifier
                .padding(start = 2.dp)
                .weight(1f)
                .height(size),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(color.copy(alpha = 0.55f)),
                )
            }
        }
    }
}

@Composable
private fun TimeLayoutSectionIcon(color: Color, size: androidx.compose.ui.unit.Dp) {
    Column(
        modifier = Modifier
            .size(size)
            .border(1.5.dp, color, RoundedCornerShape(2.dp)),
    ) {
        repeat(3) { index ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(color),
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = color,
                )
            }
        }
    }
}

@Composable
fun AgendaViewModeIcon(
    level: AgendaZoomLevel,
    modifier: Modifier = Modifier,
    color: Color = PoolColors.AccentPrimary,
) {
    Box(modifier = modifier.size(22.dp), contentAlignment = Alignment.Center) {
        when (level) {
            AgendaZoomLevel.ONE -> ViewModeHollowSquare(color = color, size = 16.dp)
            AgendaZoomLevel.THREE -> ViewModeTripleSplitSquare(color = color, size = 16.dp)
            AgendaZoomLevel.WEEK -> ViewModeSolidSquare(color = color, size = 16.dp)
        }
    }
}

@Composable
private fun ViewModeHollowSquare(color: Color, size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .border(1.5.dp, color, RoundedCornerShape(2.dp)),
    )
}

@Composable
private fun ViewModeSolidSquare(color: Color, size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(2.dp))
            .background(color),
    )
}

@Composable
private fun ViewModeTripleSplitSquare(color: Color, size: androidx.compose.ui.unit.Dp) {
    Row(
        modifier = Modifier
            .size(size)
            .border(1.5.dp, color, RoundedCornerShape(2.dp)),
    ) {
        Box(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(size)
                .background(color),
        )
        Box(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(size)
                .background(color),
        )
        Box(modifier = Modifier.weight(1f))
    }
}
