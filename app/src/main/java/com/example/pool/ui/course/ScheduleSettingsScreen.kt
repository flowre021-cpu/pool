package com.example.pool.ui.course

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pool.ui.components.PoolBlock1
import com.example.pool.ui.components.PoolDivider
import com.example.pool.ui.components.PoolNavRow
import com.example.pool.ui.theme.PoolColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleSettingsScreen(
    onBack: () -> Unit,
    onOpenSemesters: () -> Unit,
    onOpenCourseManagement: () -> Unit,
    onOpenTimeSlots: () -> Unit,
    onOpenCardColors: () -> Unit,
    onOpenImport: () -> Unit,
) {
    Scaffold(
        containerColor = PoolColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("课表设置") },
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
                    PoolNavRow(
                        title = "学期设置",
                        subtitle = "当前学期 · 教学周数 · 起止周",
                        onClick = onOpenSemesters,
                    )
                    PoolDivider()
                    PoolNavRow(
                        title = "课程管理",
                        subtitle = "查看与编辑全部课程",
                        onClick = onOpenCourseManagement,
                    )
                    PoolDivider()
                    PoolNavRow(
                        title = "从教务导入",
                        subtitle = "北航本研教育管理系统 · WebView 登录抓取",
                        onClick = onOpenImport,
                    )
                    PoolDivider()
                    PoolNavRow(
                        title = "节次时间",
                        subtitle = "各节次上下课时间",
                        onClick = onOpenTimeSlots,
                    )
                    PoolDivider()
                    PoolNavRow(
                        title = "课程颜色",
                        subtitle = "默认 / 自定义色卡切换",
                        onClick = onOpenCardColors,
                    )
                }
            }
        }
    }
}
