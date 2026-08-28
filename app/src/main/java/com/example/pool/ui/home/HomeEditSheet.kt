package com.example.pool.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pool.ui.components.PoolBlock1
import com.example.pool.ui.components.PoolDivider
import com.example.pool.ui.components.PoolNavRow
import com.example.pool.ui.theme.PoolColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeEditSheet(
    onDismiss: () -> Unit,
    onOpenAffairs: () -> Unit,
    onOpenCourses: () -> Unit,
    onOpenPreferences: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PoolColors.Background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
        ) {
            PoolBlock1 {
                Column {
                    PoolNavRow(
                        title = "事务",
                        subtitle = "待办 · 周期事项 · 日程事件 · 提醒",
                        onClick = {
                            onDismiss()
                            onOpenAffairs()
                        },
                    )
                    PoolDivider()
                    PoolNavRow(
                        title = "课表",
                        subtitle = "管理课程与上课安排",
                        onClick = {
                            onDismiss()
                            onOpenCourses()
                        },
                    )
                    PoolDivider()
                    PoolNavRow(
                        title = "外观与偏好",
                        subtitle = "默认主页 · 主题颜色",
                        onClick = {
                            onDismiss()
                            onOpenPreferences()
                        },
                    )
                }
            }
        }
    }
}
