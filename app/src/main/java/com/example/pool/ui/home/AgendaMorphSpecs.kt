package com.example.pool.ui.home

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp

/** 列宽：快启慢收，整体更短更轻 */
val AgendaMorphWidthEasing = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)

/** 透明度：略快于列宽，减少叠影感 */
val AgendaMorphOpacityEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

const val AgendaMorphWidthDurationMs = 320

const val AgendaMorphOpacityDurationMs = 200

fun agendaMorphWidthSpec(): AnimationSpec<Dp> = tween(
    durationMillis = AgendaMorphWidthDurationMs,
    easing = AgendaMorphWidthEasing,
)

fun agendaMorphOpacitySpec(): AnimationSpec<Float> = tween(
    durationMillis = AgendaMorphOpacityDurationMs,
    easing = AgendaMorphOpacityEasing,
)

fun Modifier.agendaMorphFade(alpha: Float): Modifier = this.graphicsLayer {
    this.alpha = alpha
}

fun agendaMorphTotalDurationMs(): Long =
    maxOf(AgendaMorphWidthDurationMs, AgendaMorphOpacityDurationMs).toLong() + 16L
