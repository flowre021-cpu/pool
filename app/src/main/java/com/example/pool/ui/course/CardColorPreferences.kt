package com.example.pool.ui.course

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.example.pool.ui.course.model.CourseColorPalettes
import com.example.pool.ui.course.model.colorFromArgbLong
import com.example.pool.ui.course.model.toArgbLong

/** 课程卡片色卡：默认色卡 ↔ 自定义色卡 */
class CardColorPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun usesCustomPalette(): Boolean = prefs.getBoolean(KEY_USE_CUSTOM, false)

    fun setUsesCustomPalette(useCustom: Boolean) {
        prefs.edit().putBoolean(KEY_USE_CUSTOM, useCustom).apply()
    }

    fun getCustomPaletteColors(): List<Color> =
        readColorList(KEY_CUSTOM_PALETTE).ifEmpty { migrateLegacyCustomColors() }

    fun setCustomPaletteColors(colors: List<Color>) {
        writeColorList(KEY_CUSTOM_PALETTE, colors.take(CourseColorPalettes.MAX_CUSTOM_COLORS))
    }

    fun addCustomColor(color: Color): List<Color> {
        val argb = color.toArgbLong()
        val merged = listOf(argb) +
            getCustomPaletteColors()
                .map { it.toArgbLong() }
                .filter { it != argb }
        val updated = merged.take(CourseColorPalettes.MAX_CUSTOM_COLORS).map(::colorFromArgbLong)
        setCustomPaletteColors(updated)
        setUsesCustomPalette(true)
        return updated
    }

    fun removeCustomColor(color: Color): List<Color> {
        val remaining = getCustomPaletteColors()
            .filter { it.toArgbLong() != color.toArgbLong() }
        setCustomPaletteColors(remaining)
        return remaining
    }

    fun resolveActiveColors(): List<Color> {
        val custom = getCustomPaletteColors()
        return if (usesCustomPalette() && custom.isNotEmpty()) {
            custom
        } else {
            CourseColorPalettes.defaultColors
        }
    }

    private fun migrateLegacyCustomColors(): List<Color> {
        val legacy = readColorList(KEY_LEGACY_CUSTOM_COLORS)
        if (legacy.isNotEmpty()) {
            setCustomPaletteColors(legacy)
            prefs.edit().remove(KEY_LEGACY_CUSTOM_COLORS).apply()
        }
        return legacy
    }

    private fun readColorList(key: String): List<Color> =
        prefs.getString(key, null)
            ?.split(',')
            ?.mapNotNull { token ->
                token.trim().toLongOrNull(16)?.let(::colorFromArgbLong)
            }
            .orEmpty()

    private fun writeColorList(key: String, colors: List<Color>) {
        prefs.edit()
            .putString(
                key,
                if (colors.isEmpty()) null else colors.joinToString(",") { "%08X".format(it.toArgbLong()) },
            )
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "card_color_prefs"
        const val KEY_USE_CUSTOM = "use_custom_palette"
        const val KEY_CUSTOM_PALETTE = "custom_palette_colors"
        const val KEY_LEGACY_CUSTOM_COLORS = "custom_colors"
    }
}

internal fun mergeCardPalette(custom: List<Color>, builtIn: List<Color>): List<Color> {
    val seen = mutableSetOf<Long>()
    return buildList {
        custom.forEach { color ->
            val argb = color.toArgbLong()
            if (seen.add(argb)) add(color)
        }
        builtIn.forEach { color ->
            val argb = color.toArgbLong()
            if (seen.add(argb)) add(color)
        }
    }
}
