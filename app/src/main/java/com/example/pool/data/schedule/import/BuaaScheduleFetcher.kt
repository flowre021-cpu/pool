package com.example.pool.data.schedule.import

import android.webkit.CookieManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** 使用 WebView 登录态（Cookie）在北航教务 API 按周拉取课表。 */
object BuaaScheduleFetcher {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun fetchSchedule(
        pageUrl: String,
        termCode: String,
        fetchMode: BuaaScheduleFetchMode,
        onProgress: suspend (Int, Int) -> Unit,
    ): BuaaScheduleFetchResult = withContext(Dispatchers.IO) {
        when (fetchMode) {
            BuaaScheduleFetchMode.BY_WEEK -> BuaaScheduleFetchResult.ByWeek(
                fetchAllWeeks(pageUrl, termCode, onProgress),
            )
            BuaaScheduleFetchMode.BY_CLASS -> BuaaScheduleFetchResult.ByClass(
                fetchClassSchedule(pageUrl, termCode, onProgress),
            )
        }
    }

    private suspend fun fetchClassSchedule(
        pageUrl: String,
        termCode: String,
        onProgress: suspend (Int, Int) -> Unit,
    ): String = withContext(Dispatchers.IO) {
        require(termCode.isNotBlank()) { "请填写学期代码" }
        val cookies = readWebViewCookies(pageUrl)
        if (cookies.isBlank()) {
            error("未读取到登录 Cookie，请先在下方网页完成登录")
        }
        onProgress(0, 1)
        val apiUrl = probeApiUrl(pageUrl, termCode, cookies, BuaaScheduleFetchMode.BY_CLASS)
        val body = fetchScheduleRequest(
            apiUrl = apiUrl,
            pageUrl = pageUrl,
            termCode = termCode,
            fetchMode = BuaaScheduleFetchMode.BY_CLASS,
            week = null,
            cookies = cookies,
        )
        validateResponse(body, week = null)
        onProgress(1, 1)
        body
    }

    private suspend fun fetchAllWeeks(
        pageUrl: String,
        termCode: String,
        onProgress: suspend (Int, Int) -> Unit,
    ): List<WeeklyResponse> = withContext(Dispatchers.IO) {
        require(termCode.isNotBlank()) { "请填写学期代码" }
        val cookies = readWebViewCookies(pageUrl)
        if (cookies.isBlank()) {
            error("未读取到登录 Cookie，请先在下方网页完成登录")
        }
        val apiUrl = probeApiUrl(pageUrl, termCode, cookies, BuaaScheduleFetchMode.BY_WEEK)
        onProgress(0, 1)
        val firstBody = fetchScheduleRequest(
            apiUrl, pageUrl, termCode, BuaaScheduleFetchMode.BY_WEEK, week = 1, cookies,
        )
        validateResponse(firstBody, week = 1)
        val totalWeeks = (
            BuaaZhengfangParser.inferMaxWeeksFromResponse(firstBody)
                ?: BuaaScheduleImportConfig.DEFAULT_MAX_WEEKS
            ).coerceIn(1, 30)
        onProgress(1, totalWeeks)
        buildList {
            add(WeeklyResponse(week = 1, body = firstBody))
            for (week in 2..totalWeeks) {
                onProgress(week, totalWeeks)
                val body = fetchScheduleRequest(
                    apiUrl, pageUrl, termCode, BuaaScheduleFetchMode.BY_WEEK, week, cookies,
                )
                add(WeeklyResponse(week = week, body = body))
            }
        }
    }

    private fun probeApiUrl(
        pageUrl: String,
        termCode: String,
        cookies: String,
        fetchMode: BuaaScheduleFetchMode,
    ): String {
        var lastError = "未知错误"
        for (candidate in apiCandidates(pageUrl)) {
            runCatching {
                val text = fetchScheduleRequest(
                    apiUrl = candidate,
                    pageUrl = pageUrl,
                    termCode = termCode,
                    fetchMode = fetchMode,
                    week = if (fetchMode == BuaaScheduleFetchMode.BY_WEEK) 1 else null,
                    cookies = cookies,
                )
                validateResponse(text, week = if (fetchMode == BuaaScheduleFetchMode.BY_WEEK) 1 else null)
                return candidate
            }.onFailure { lastError = it.message ?: lastError }
        }
        error("课表接口不可用：$lastError")
    }

    private fun fetchScheduleRequest(
        apiUrl: String,
        pageUrl: String,
        termCode: String,
        fetchMode: BuaaScheduleFetchMode,
        week: Int?,
        cookies: String,
    ): String {
        val formBuilder = FormBody.Builder()
            .add("termCode", termCode)
            .add("campusCode", "")
            .add("type", fetchMode.typeParam)
        if (fetchMode == BuaaScheduleFetchMode.BY_WEEK) {
            formBuilder.add("week", (week ?: error("按周拉取缺少 week")).toString())
        }
        val form = formBuilder.build()
        val request = Request.Builder()
            .url(apiUrl)
            .post(form)
            .header("Cookie", cookies)
            .header("Referer", pageUrl)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 PoolScheduleImport/1.1",
            )
            .header("Accept", "application/json, text/plain, */*")
            .build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val label = week?.let { "第 $it 周" } ?: "课表"
                error("HTTP ${response.code}（$label）")
            }
            return text
        }
    }

    internal fun validateResponse(text: String, week: Int?) {
        if (text.isBlank()) {
            error(week?.let { "第 $it 周返回空内容" } ?: "课表返回空内容")
        }
        if (text.trimStart().startsWith("<")) {
            error(
                week?.let { "第 $it 周返回网页而非 JSON，登录可能已失效，请刷新网页重新登录" }
                    ?: "返回网页而非 JSON，登录可能已失效，请刷新网页重新登录",
            )
        }
        val json = JSONObject(text)
        if (json.optString("code") != "0") {
            val label = week?.let { "第 $it 周" } ?: "课表"
            error(json.optString("msg", "$label 接口错误 code=${json.optString("code")}"))
        }
    }

    internal fun apiCandidates(pageUrl: String): List<String> {
        val host = runCatching { java.net.URI(pageUrl).host }.getOrNull() ?: "byxt.buaa.edu.cn"
        val schemes = linkedSetOf("https", "http")
        runCatching { java.net.URI(pageUrl).scheme?.lowercase() }.getOrNull()?.let { schemes.add(it) }
        val paths = listOf(
            "/jwapp/sys/homeapp/api/home/student/getMyScheduleDetail.do",
            "/jwapp/sys/homeapp/api/student/getMyScheduleDetail.do",
            "/jwapp/sys/jwsybuaa/api/home/student/getMyScheduleDetail.do",
        )
        return schemes.flatMap { scheme ->
            paths.map { path -> "$scheme://$host$path" }
        }.distinct()
    }

    internal fun readWebViewCookies(pageUrl: String): String {
        CookieManager.getInstance().flush()
        val manager = CookieManager.getInstance()
        val host = runCatching { java.net.URI(pageUrl).host }.getOrNull() ?: "byxt.buaa.edu.cn"
        val chunks = linkedSetOf<String>()
        listOf(
            pageUrl,
            "https://$host/",
            "http://$host/",
            "https://byxt.buaa.edu.cn/",
            "http://byxt.buaa.edu.cn/",
        ).forEach { url ->
            manager.getCookie(url)?.takeIf { it.isNotBlank() }?.let { raw ->
                raw.split(";").forEach { part ->
                    val trimmed = part.trim()
                    if (trimmed.isNotEmpty()) chunks.add(trimmed)
                }
            }
        }
        return chunks.joinToString("; ")
    }
}

sealed class BuaaScheduleFetchResult {
    data class ByWeek(val responses: List<WeeklyResponse>) : BuaaScheduleFetchResult()
    data class ByClass(val body: String) : BuaaScheduleFetchResult()
}

fun BuaaScheduleFetchResult.toImportedSlots(): List<ImportedCourseSlot> = when (this) {
    is BuaaScheduleFetchResult.ByWeek -> BuaaZhengfangParser.mergeWeeklyResponses(responses)
    is BuaaScheduleFetchResult.ByClass -> BuaaZhengfangParser.mergeClassResponse(body)
}
