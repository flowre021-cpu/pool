package com.example.pool.ui.affair

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.pool.data.AffairType
import com.example.pool.data.PlannerRepository
import com.example.pool.ui.components.PoolAddFab
import com.example.pool.ui.theme.PoolColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AffairTypeListScreen(
    affairType: AffairType,
    repository: PlannerRepository,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    Scaffold(
        containerColor = PoolColors.Background,
        topBar = {
            TopAppBar(
                title = { Text(affairType.hubTitle()) },
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
        floatingActionButton = {
            PoolAddFab(
                onClick = onAdd,
                contentDescription = affairType.addActionLabel(),
            )
        },
        floatingActionButtonPosition = FabPosition.End,
    ) { padding ->
        AffairListScreen(
            affairType = affairType,
            repository = repository,
            onEdit = onEdit,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        )
    }
}
