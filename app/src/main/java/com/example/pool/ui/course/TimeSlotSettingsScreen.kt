package com.example.pool.ui.course

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pool.data.schedule.ScheduleTimeSlot
import com.example.pool.ui.components.PoolBlock1
import com.example.pool.ui.components.PoolRowHorizontalPadding
import com.example.pool.ui.course.components.CourseEditActionText
import com.example.pool.ui.course.components.EditSingleTimeSlotDialog
import com.example.pool.ui.course.model.TimeSlot
import com.example.pool.ui.course.model.toUiTimeSlot
import com.example.pool.ui.theme.PoolColors
import com.example.pool.util.formatMinutesOfDay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeSlotSettingsScreen(
    viewModel: CourseViewModel,
    onBack: () -> Unit,
) {
    val timeSlotEntities by viewModel.timeSlots.collectAsStateWithLifecycle()
    val timeSlots = remember(timeSlotEntities) { timeSlotEntities.map { it.toUiTimeSlot() } }
    var editingTimeSlot by remember { mutableStateOf<TimeSlot?>(null) }

    editingTimeSlot?.let { slot ->
        EditSingleTimeSlotDialog(
            slot = slot,
            onDismiss = { editingTimeSlot = null },
            onSave = { updated ->
                viewModel.updateTimeSlot(
                    ScheduleTimeSlot(
                        sectionNumber = updated.sectionNumber,
                        startTimeMinutes = updated.startTimeMinutes,
                        endTimeMinutes = updated.endTimeMinutes,
                    ),
                )
                editingTimeSlot = null
            },
        )
    }

    Scaffold(
        containerColor = PoolColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("节次时间") },
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
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            PoolBlock1(modifier = Modifier.padding(top = 8.dp)) {
                Column(modifier = Modifier.padding(vertical = 16.dp)) {
                    Text(
                        text = "节次列表",
                        style = ScheduleFormStyles.sectionTitle,
                        modifier = Modifier.padding(horizontal = PoolRowHorizontalPadding),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PoolRowHorizontalPadding),
                        shape = RoundedCornerShape(10.dp),
                        color = ScheduleFormStyles.cardBackground,
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
                            timeSlots.forEachIndexed { index, slot ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { editingTimeSlot = slot }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "第 ${slot.sectionNumber} 节  " +
                                            "${formatMinutesOfDay(slot.startTimeMinutes)} - " +
                                            formatMinutesOfDay(slot.endTimeMinutes),
                                        style = ScheduleFormStyles.body,
                                        modifier = Modifier.weight(1f),
                                    )
                                    CourseEditActionText(
                                        text = "修改节次",
                                        onClick = { editingTimeSlot = slot },
                                    )
                                }
                                if (index < timeSlots.lastIndex) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
