package com.example.pool.ui.components

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.pool.ui.theme.PoolColors

@Composable
fun PoolSelectionTopBarActions(
    selectedCount: Int,
    onDelete: () -> Unit,
    deleteLabel: String = "删除",
) {
    TextButton(onClick = onDelete, enabled = selectedCount > 0) {
        Text(
            text = if (selectedCount > 0) "$deleteLabel($selectedCount)" else deleteLabel,
            color = if (selectedCount > 0) PoolColors.DeleteRed else PoolColors.TextSecondary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
