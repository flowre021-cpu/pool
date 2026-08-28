package com.example.pool.ui.home

import java.time.LocalDate
import java.util.LinkedHashMap

internal data class AgendaWeekCacheKey(
    val anchorEpochDay: Long,
    val contentRevision: Long,
)

/** 仅缓存完整周 UI 状态；视图档位在 UI 层通过列宽动画表现 */
internal class AgendaPageStateCache(
    private val maxSize: Int = 24,
) {
    private val cache = object : LinkedHashMap<AgendaWeekCacheKey, HomeUiState>(maxSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<AgendaWeekCacheKey, HomeUiState>?): Boolean {
            return size > maxSize
        }
    }

    @Synchronized
    fun getFullWeekOrBuild(inputs: HomeAgendaInputs, anchorDate: LocalDate): HomeUiState {
        if (!inputs.hasAgendaData()) {
            return HomeUiState(anchorDate = anchorDate)
        }
        val revision = inputs.contentRevision()
        val key = AgendaWeekCacheKey(anchorDate.toEpochDay(), revision)
        cache[key]?.let { return it }
        cache.keys.removeIf { it.contentRevision != revision }
        val built = buildHomeUiState(inputs, anchorDate)
        cache[key] = built
        return built
    }

    @Synchronized
    fun clear() {
        cache.clear()
    }
}
