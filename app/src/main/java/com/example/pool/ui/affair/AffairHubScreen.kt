package com.example.pool.ui.affair

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
import com.example.pool.data.AffairType
import com.example.pool.ui.components.PoolBlock1
import com.example.pool.ui.components.PoolDivider
import com.example.pool.ui.components.PoolNavRow
import com.example.pool.ui.theme.PoolColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AffairHubScreen(
    onBack: () -> Unit,
    onOpenType: (AffairType) -> Unit,
) {
    Scaffold(
        containerColor = PoolColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("事务") },
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
                    AffairHubTypes.forEachIndexed { index, type ->
                        PoolNavRow(
                            title = type.hubTitle(),
                            subtitle = type.hubSubtitle(),
                            onClick = { onOpenType(type) },
                        )
                        if (index < AffairHubTypes.lastIndex) {
                            PoolDivider()
                        }
                    }
                }
            }
        }
    }
}
