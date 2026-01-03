package com.example.uiedvideocompacter.ui.screens.result

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.uiedvideocompacter.R
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    viewModel: ResultViewModel = viewModel(),
    onNavigateHome: () -> Unit
) {
    val results by viewModel.results.collectAsState()
    val successCount by viewModel.successCount.collectAsState()
    val failCount by viewModel.failCount.collectAsState()
    val deleteRequest by viewModel.deleteRequest.collectAsState()
    
    var hasInitialized by remember { mutableStateOf(false) }
    var pendingReplacementItem by remember { mutableStateOf<ResultItem?>(null) }
    var previewItem by remember { mutableStateOf<ResultItem?>(null) }

    // 全てのタスクが終了（リストが空になった）したら自動的にホームに戻る
    LaunchedEffect(results) {
        if (results.isNotEmpty()) {
            hasInitialized = true
        } else if (hasInitialized && results.isEmpty()) {
            onNavigateHome()
        }
    }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        if (activityResult.resultCode == android.app.Activity.RESULT_OK) {
            pendingReplacementItem?.let { item ->
                viewModel.finalizeReplacement(item)
            }
        }
        pendingReplacementItem = null
    }

    LaunchedEffect(deleteRequest) {
        deleteRequest?.let { pendingIntent ->
            deleteLauncher.launch(IntentSenderRequest.Builder(pendingIntent).build())
            viewModel.consumeDeleteRequest()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadResults()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_results)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateHome) {
                        Icon(Icons.Default.Home, contentDescription = stringResource(R.string.home))
                    }
                },
                actions = {
                    if (results.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearAll() }) {
                            Text(stringResource(R.string.clear_all))
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (results.isNotEmpty()) {
                Surface(
                    tonalElevation = 3.dp,
                    shadowElevation = 8.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "$successCount ${stringResource(R.string.status_completed)}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (failCount > 0) {
                                    Text(
                                        "$failCount ${stringResource(R.string.status_failed)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (results.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.no_results),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(results) { result ->
                    ResultItemRow(
                        result = result,
                        onPreview = { previewItem = result },
                        onShare = { viewModel.shareResult(result) },
                        onSave = { viewModel.saveToGallery(result) },
                        onReplace = {
                            pendingReplacementItem = result
                            viewModel.prepareReplaceOriginal(result)
                        },
                        onDelete = { viewModel.deleteResult(result) }
                    )
                }
            }
        }
    }

    // Result Preview Dialog
    previewItem?.let { item ->
        ResultPreviewDialog(
            item = item,
            onDismiss = { previewItem = null },
            onReplace = {
                previewItem = null
                pendingReplacementItem = item
                viewModel.prepareReplaceOriginal(item)
            },
            onSave = {
                previewItem = null
                viewModel.saveToGallery(item)
            }
        )
    }
}

@Composable
fun ResultPreviewDialog(
    item: ResultItem,
    onDismiss: () -> Unit,
    onReplace: () -> Unit,
    onSave: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(android.net.Uri.fromFile(File(item.outputPath))))
            volume = 0f // Mute by default
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = Color.White)
                    }
                    Text(
                        "圧縮後のプレビュー",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.width(48.dp))
                }

                // Player
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                    )
                }

                // Actions
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            item.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "サイズ: ${item.compressedSizeFormatted} (${item.savedPercent} 節約)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onReplace,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.replace_original), style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ResultItemRow(
    result: ResultItem,
    onPreview: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onReplace: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (result.isSuccess) Icons.Default.CheckCircle else Icons.Default.Close,
                        contentDescription = null,
                        tint = if (result.isSuccess) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        result.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (result.isSuccess) {
                    IconButton(onClick = onPreview) {
                        Icon(Icons.Default.PlayArrow, "Preview", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        stringResource(R.string.original_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(result.originalSizeFormatted, style = MaterialTheme.typography.bodySmall)
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp).align(Alignment.CenterVertically),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        stringResource(R.string.compressed_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(result.compressedSizeFormatted, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (result.isSuccess) {
                Text(
                    stringResource(R.string.savings, result.savedPercent),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                Text(
                    stringResource(R.string.error_prefix, result.errorMessage ?: stringResource(R.string.unknown)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (result.isSuccess) {
                    Button(
                        onClick = { onReplace() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.replace_original))
                    }
                } else {
                    IconButton(onClick = { onDelete() }) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
