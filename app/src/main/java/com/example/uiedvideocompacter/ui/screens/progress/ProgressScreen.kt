package com.example.uiedvideocompacter.ui.screens.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.WorkInfo
import com.example.uiedvideocompacter.R
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    viewModel: ProgressViewModel = viewModel(),
    onNavigateToResult: () -> Unit,
    onBack: () -> Unit
) {
    val workInfos by viewModel.compressionWorkInfos.observeAsState(initial = emptyList())
    val currentView = LocalView.current

    // Keep screen on while processing
    DisposableEffect(Unit) {
        currentView.keepScreenOn = true
        onDispose {
            currentView.keepScreenOn = false
        }
    }

    LaunchedEffect(workInfos) {
        val allFinished = workInfos.isNotEmpty() && workInfos.all { it.state.isFinished }
        if (allFinished) {
            onNavigateToResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_active_tasks)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        if (workInfos.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    stringResource(R.string.no_active_tasks),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            LazyColumn(modifier = Modifier.padding(innerPadding)) {
                items(workInfos) { workInfo ->
                    WorkInfoItem(
                        workInfo = workInfo,
                        onCancel = { viewModel.cancelWork(workInfo.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun WorkInfoItem(
    workInfo: WorkInfo,
    onCancel: () -> Unit
) {
    val progress = workInfo.progress.getInt("progress", 0)
    val isFinished = workInfo.state.isFinished
    val isSucceeded = workInfo.state == WorkInfo.State.SUCCEEDED
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        stringResource(R.string.notification_channel_name),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        when (workInfo.state) {
                            WorkInfo.State.RUNNING -> stringResource(R.string.status_processing)
                            WorkInfo.State.SUCCEEDED -> stringResource(R.string.status_completed)
                            WorkInfo.State.FAILED -> stringResource(R.string.status_failed)
                            WorkInfo.State.CANCELLED -> stringResource(R.string.status_cancelled)
                            else -> stringResource(R.string.status_pending)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSucceeded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (!isFinished) {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel), tint = MaterialTheme.colorScheme.error)
                    }
                } else if (isSucceeded) {
                     Icon(Icons.Default.CheckCircle, contentDescription = stringResource(R.string.done), tint = MaterialTheme.colorScheme.primary)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (!isFinished) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth().height(8.dp).globalClip(RoundedCornerShape(4.dp)),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "$progress%", 
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.End)
                )
            }
            
            if (isSucceeded) {
                val outputPath = workInfo.outputData.getString("output_path") ?: stringResource(R.string.unknown)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text(
                        stringResource(R.string.saved_to, outputPath),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            } else if (workInfo.state == WorkInfo.State.FAILED) {
                val errorMessage = workInfo.outputData.getString("error") ?: stringResource(R.string.unknown)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text(
                        stringResource(R.string.error_prefix, errorMessage),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

// Helper extension
private fun Modifier.globalClip(shape: androidx.compose.ui.graphics.Shape): Modifier = this.clip(shape)
