package com.example.pool.ui.course.importing

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pool.data.schedule.ScheduleRepository
import com.example.pool.data.schedule.import.BuaaScheduleImportConfig
import com.example.pool.data.schedule.import.BuaaTermSeason
import com.example.pool.ui.components.PoolBlock1
import com.example.pool.ui.components.PoolDivider
import com.example.pool.ui.components.PoolNavRow
import com.example.pool.ui.components.PoolPreferenceOptionRow
import com.example.pool.ui.components.PoolRowHorizontalPadding
import com.example.pool.ui.course.components.CourseEditActionText
import com.example.pool.ui.theme.PoolColors
import com.example.pool.widget.WidgetRefreshScheduler

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleImportScreen(
    scheduleRepository: ScheduleRepository,
    onBack: () -> Unit,
    onImported: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: ScheduleImportViewModel = viewModel(
        factory = ScheduleImportViewModelFactory(scheduleRepository),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var showTermPicker by remember { mutableStateOf(false) }
    val controlsEnabled = uiState.phase != ImportPhase.Fetching && uiState.phase != ImportPhase.Importing

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.destroy()
            webViewRef = null
        }
    }

    if (uiState.phase == ImportPhase.Preview) {
        ImportPreviewDialog(
            termLabel = uiState.termLabel,
            slots = uiState.previewSlots,
            onDismiss = viewModel::resetPreview,
            onConfirm = {
                viewModel.confirmImport { message ->
                    WidgetRefreshScheduler.requestRefresh(context)
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    onImported()
                }
            },
        )
    }

    if (showTermPicker) {
        TermPickerDialog(
            yearOptions = viewModel.yearOptions,
            selectedYear = uiState.calendarYear,
            selectedSeason = uiState.season,
            onSelectYear = viewModel::updateCalendarYear,
            onSelectSeason = viewModel::updateSeason,
            onDismiss = { showTermPicker = false },
        )
    }

    Scaffold(
        containerColor = PoolColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("从教务导入") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = PoolColors.AccentPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PoolColors.NavBar,
                    titleContentColor = PoolColors.TextPrimary,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            PoolBlock1(modifier = Modifier.padding(top = 12.dp)) {
                Column {
                    if (controlsEnabled) {
                        PoolNavRow(
                            title = "导入学期",
                            subtitle = uiState.termLabel,
                            onClick = { showTermPicker = true },
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = PoolRowHorizontalPadding, vertical = 14.dp),
                        ) {
                            Text(
                                text = "导入学期",
                                style = com.example.pool.ui.course.ScheduleFormStyles.sectionTitle,
                                color = PoolColors.AccentPrimary,
                            )
                            Text(
                                text = uiState.termLabel,
                                style = com.example.pool.ui.course.ScheduleFormStyles.body,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                    PoolDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PoolRowHorizontalPadding, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "抓取课表",
                                style = com.example.pool.ui.course.ScheduleFormStyles.sectionTitle,
                                color = PoolColors.AccentPrimary,
                            )
                            Text(
                                text = "在下方网页登录后点击开始",
                                style = com.example.pool.ui.course.ScheduleFormStyles.secondary,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        if (uiState.phase == ImportPhase.Fetching || uiState.phase == ImportPhase.Importing) {
                            CircularProgressIndicator(modifier = Modifier.height(24.dp))
                        } else {
                            CourseEditActionText(
                                text = "开始",
                                onClick = {
                                    val webView = webViewRef ?: return@CourseEditActionText
                                    CookieManager.getInstance().flush()
                                    val pageUrl = webView.url?.takeIf { it.isNotBlank() }
                                        ?: BuaaScheduleImportConfig.LOGIN_URL
                                    viewModel.fetchSchedule(pageUrl)
                                },
                            )
                        }
                    }
                    uiState.progressText?.let { progress ->
                        Text(
                            text = progress,
                            color = PoolColors.AccentPrimary,
                            style = com.example.pool.ui.course.ScheduleFormStyles.secondary,
                            modifier = Modifier.padding(
                                horizontal = PoolRowHorizontalPadding,
                                vertical = 8.dp,
                            ),
                        )
                    }
                    uiState.errorMessage?.let { message ->
                        Text(
                            text = message,
                            color = PoolColors.DeleteRed,
                            style = com.example.pool.ui.course.ScheduleFormStyles.secondary,
                            modifier = Modifier.padding(
                                horizontal = PoolRowHorizontalPadding,
                                vertical = 8.dp,
                            ),
                        )
                    }
                }
            }
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        webViewClient = WebViewClient()
                        webChromeClient = WebChromeClient()
                        configureImportWebView(this)
                        loadUrl(BuaaScheduleImportConfig.LOGIN_URL)
                        webViewRef = this
                    }
                },
            )
        }
    }
}

@Composable
private fun TermPickerDialog(
    yearOptions: List<Int>,
    selectedYear: Int,
    selectedSeason: BuaaTermSeason,
    onSelectYear: (Int) -> Unit,
    onSelectSeason: (BuaaTermSeason) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PoolColors.Block1,
        titleContentColor = PoolColors.TextPrimary,
        textContentColor = PoolColors.TextPrimary,
        title = { Text("选择导入学期") },
        text = {
            Column {
                Text(text = "年份", color = PoolColors.TextSecondary)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp),
                ) {
                    items(yearOptions) { year ->
                        PoolPreferenceOptionRow(
                            label = "${year} 年",
                            selected = selectedYear == year,
                            onSelect = { onSelectYear(year) },
                        )
                        if (year != yearOptions.last()) {
                            PoolDivider()
                        }
                    }
                }
                Text(
                    text = "学期",
                    color = PoolColors.TextSecondary,
                    modifier = Modifier.padding(top = 12.dp),
                )
                BuaaTermSeason.entries.forEachIndexed { index, season ->
                    PoolPreferenceOptionRow(
                        label = season.label + "学期",
                        selected = selectedSeason == season,
                        onSelect = { onSelectSeason(season) },
                    )
                    if (index < BuaaTermSeason.entries.lastIndex) {
                        PoolDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("完成", color = PoolColors.AccentPrimary)
            }
        },
    )
}

@Composable
private fun ImportPreviewDialog(
    termLabel: String,
    slots: List<com.example.pool.data.schedule.import.ImportedCourseSlot>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val groupCount = slots.map { it.groupId }.distinct().size
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PoolColors.Block1,
        titleContentColor = PoolColors.TextPrimary,
        textContentColor = PoolColors.TextPrimary,
        title = { Text("导入预览") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("学期：$termLabel")
                Text("共 ${groupCount} 门课，${slots.size} 个上课时段")
                Text("将覆盖该学期已有课程，其他学期不受影响。")
                LazyColumn(
                    modifier = Modifier.height(220.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(slots.take(30)) { slot ->
                        Text(
                            text = "${slot.name} · 周${slot.dayOfWeek} · " +
                                "${slot.startSection}-${slot.endSection}节 · " +
                                "${slot.selectedWeeks.size}周",
                            color = PoolColors.TextSecondary,
                        )
                    }
                    if (slots.size > 30) {
                        item {
                            Text("… 还有 ${slots.size - 30} 条", color = PoolColors.TextSecondary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("确认导入", color = PoolColors.AccentPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = PoolColors.TextPrimary)
            }
        },
    )
}

private fun configureImportWebView(webView: WebView) {
    val settings = webView.settings
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.databaseEnabled = true
    settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
    settings.userAgentString = settings.userAgentString + " PoolScheduleImport/1.1"
}
