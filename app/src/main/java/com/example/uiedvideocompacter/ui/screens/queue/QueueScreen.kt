package com.example.uiedvideocompacter.ui.screens.queue

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uiedvideocompacter.data.model.CompressionPreset
import com.example.uiedvideocompacter.ui.components.VideoThumbnail
import androidx.compose.ui.res.stringResource
import com.example.uiedvideocompacter.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    viewModel: QueueViewModel = viewModel(),
    onNavigateToProgress: () -> Unit,
    onBack: () -> Unit
) {
    val queueItems by viewModel.queueItems.collectAsState()
    val totalSize by viewModel.totalSize.collectAsState()
    val estimatedSize by viewModel.estimatedSize.collectAsState()
    val savings by viewModel.savings.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadQueue()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_queue)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        bottomBar = {
            if (queueItems.isNotEmpty()) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(R.string.total_size, totalSize))
                            Text(stringResource(R.string.estimated_output, estimatedSize))
                        }
                        Text(
                            stringResource(R.string.savings, savings),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.End)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.startCompression(onNavigateToProgress) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.start_compression))
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (queueItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.queue_empty), style = MaterialTheme.typography.titleMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(queueItems) { item ->
                    QueueItemRow(
                        item = item,
                        onRemove = { viewModel.removeFromQueue(item.id) },
                        onUpdatePreset = { preset -> viewModel.updatePreset(item.id, preset) }
                    )
                }
            }
        }
    }
}

@Composable
fun QueueItemRow(
    item: QueueItem,
    onRemove: () -> Unit,
    onUpdatePreset: (CompressionPreset) -> Unit
) {
    var showPresetMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VideoThumbnail(
                uri = Uri.parse(item.uri),
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = "${item.sizeFormatted} • ${item.durationFormatted}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box {
                    AssistChip(
                        onClick = { if (item.targetPercentage == null) showPresetMenu = true },
                        label = { Text(item.compressionLabel) },
                        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, Modifier.size(16.dp)) }
                    )
                    DropdownMenu(
                        expanded = showPresetMenu,
                        onDismissRequest = { showPresetMenu = false }
                    ) {
                        CompressionPreset.values().forEach { preset ->
                            DropdownMenuItem(
                                text = { 
                                    Column {
                                        Text(preset.title)
                                        Text(preset.description, style = MaterialTheme.typography.labelSmall)
                                    }
                                },
                                onClick = {
                                    onUpdatePreset(preset)
                                    showPresetMenu = false
                                }
                            )
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.estimated_output, item.estimatedSizeFormatted),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.remove_item), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}