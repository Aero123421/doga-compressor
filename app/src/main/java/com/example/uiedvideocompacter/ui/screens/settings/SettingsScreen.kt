package com.example.uiedvideocompacter.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uiedvideocompacter.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onBack: () -> Unit
) {
    val showResolution by viewModel.showResolution.collectAsState()
    val maxSelection by viewModel.maxSelection.collectAsState()
    val maxParallelTasks by viewModel.maxParallelTasks.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            
            // Show Resolution Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.show_resolution), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.show_resolution_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = showResolution,
                    onCheckedChange = { viewModel.toggleShowResolution(it) }
                )
            }
            
            // Max Selection
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.max_selection), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.max_selection_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    Button(
                        onClick = { if (maxSelection > 1) viewModel.updateMaxSelection(maxSelection - 1) },
                        enabled = maxSelection > 1
                    ) {
                        Text("-")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        maxSelection.toString(),
                        modifier = Modifier.align(Alignment.CenterVertically).padding(horizontal = 8.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.updateMaxSelection(maxSelection + 1) }
                    ) {
                        Text("+")
                    }
                }
            }

            // Max Parallel Tasks
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("並列実行数", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "同時に実行する圧縮タスクの最大数 (1-3)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    Button(
                        onClick = { if (maxParallelTasks > 1) viewModel.setMaxParallelTasks(maxParallelTasks - 1) },
                        enabled = maxParallelTasks > 1
                    ) {
                        Text("-")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        maxParallelTasks.toString(),
                        modifier = Modifier.align(Alignment.CenterVertically).padding(horizontal = 8.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { if (maxParallelTasks < 3) viewModel.setMaxParallelTasks(maxParallelTasks + 1) },
                        enabled = maxParallelTasks < 3
                    ) {
                        Text("+")
                    }
                }
            }
            
            // Version Info
            Spacer(modifier = Modifier.weight(1f))
            Text(
                stringResource(R.string.version, "1.0.0"),
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}