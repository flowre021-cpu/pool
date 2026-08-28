package com.example.pool.ui.task.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pool.ui.components.PoolRowHorizontalPadding
import com.example.pool.ui.course.components.CourseEditActionText
import com.example.pool.ui.theme.PoolColors
import com.example.pool.util.formatDeadlineDate
import com.example.pool.util.formatDeadlineTime

private val FormSectionHorizontalPadding = PoolRowHorizontalPadding

private val FormSectionTitleStyle = TextStyle(
    fontSize = 15.sp,
    fontWeight = FontWeight.Medium,
    color = PoolColors.TextPrimary,
)

private val FormInputTextStyle = TextStyle(
    fontSize = 14.sp,
    fontWeight = FontWeight.Normal,
    color = PoolColors.TextPrimary,
)

private val FormPlaceholderStyle = TextStyle(
    fontSize = 14.sp,
    fontWeight = FontWeight.Normal,
    color = PoolColors.TextSecondary,
)

private val FormHelperTextStyle = TextStyle(
    fontSize = 12.sp,
    fontWeight = FontWeight.Normal,
    color = PoolColors.TextSecondary,
)

private val FormDdlValueStyle = TextStyle(
    fontSize = 14.sp,
    fontWeight = FontWeight.Medium,
    color = PoolColors.TextPrimary,
)

private val FormDdlPlaceholderStyle = TextStyle(
    fontSize = 14.sp,
    fontWeight = FontWeight.Normal,
    color = PoolColors.TextSecondary,
)

@Composable
fun TaskEditTextFieldSection(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else 4,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FormSectionHorizontalPadding, vertical = 16.dp),
    ) {
        Text(text = title, style = FormSectionTitleStyle)
        Spacer(modifier = Modifier.height(12.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = FormInputTextStyle,
            singleLine = singleLine,
            maxLines = maxLines,
            cursorBrush = SolidColor(PoolColors.Accent),
            decorationBox = { innerTextField ->
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(text = placeholder, style = FormPlaceholderStyle)
                }
                innerTextField()
            },
        )
    }
}

@Composable
fun TaskEditNoteSection(
    value: String,
    onValueChange: (String) -> Unit,
    helperText: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FormSectionHorizontalPadding, vertical = 16.dp),
    ) {
        Text(text = "备注", style = FormSectionTitleStyle)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = helperText, style = FormHelperTextStyle)
        Spacer(modifier = Modifier.height(12.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp),
            textStyle = FormInputTextStyle,
            singleLine = false,
            maxLines = 4,
            cursorBrush = SolidColor(PoolColors.Accent),
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(text = "（可不填）", style = FormPlaceholderStyle)
                }
                innerTextField()
            },
        )
    }
}

@Composable
fun TaskEditDdlSection(
    hasDeadline: Boolean,
    deadlineMillis: Long,
    onSetDateClick: () -> Unit,
    onSetTimeClick: () -> Unit,
    onClear: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FormSectionHorizontalPadding, vertical = 16.dp),
    ) {
        Text(text = "截止时间 (DDL)", style = FormSectionTitleStyle)
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = when {
                    hasDeadline -> "${formatDeadlineDate(deadlineMillis)}  ${formatDeadlineTime(deadlineMillis)}"
                    else -> "未设置"
                },
                style = if (hasDeadline) FormDdlValueStyle else FormDdlPlaceholderStyle,
                modifier = Modifier.weight(1f),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CourseEditActionText(text = "设置日期", onClick = onSetDateClick)
                CourseEditActionText(text = "设置时间", onClick = onSetTimeClick)
                if (hasDeadline) {
                    CourseEditActionText(text = "清除", onClick = onClear)
                }
            }
        }
    }
}
