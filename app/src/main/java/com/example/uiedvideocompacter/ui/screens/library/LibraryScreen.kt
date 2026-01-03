package com.example.uiedvideocompacter.ui.screens.library

import android.Manifest
import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.uiedvideocompacter.R
import com.example.uiedvideocompacter.data.model.VideoItem
import com.example.uiedvideocompacter.ui.components.VideoThumbnail
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = viewModel(),
    onNavigateToPreview: (String) -> Unit,
    onNavigateToQueue: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val permissionString = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    
    val permissionState = rememberPermissionState(permissionString)
    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }
    
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val videos by viewModel.videos.collectAsState()
    val selectedVideos by viewModel.selectedVideos.collectAsState(initial = emptySet<Long>())
    val showResolution by viewModel.showResolution.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val isLoading = viewModel.isLoading

    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchActive by viewModel.searchActive.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    
    var showSortMenu by remember { mutableStateOf(false) }
    var previewVideo by remember { mutableStateOf<VideoItem?>(null) }

    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()

    // 並び替え時にスクロール位置をリセット
    LaunchedEffect(sortOrder) {
        gridState.scrollToItem(0)
        listState.scrollToItem(0)
    }

    LaunchedEffect(permissionState.status.isGranted) {
        if (permissionState.status.isGranted) {
            viewModel.loadVideos(reset = true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (notificationPermissionState?.status?.isGranted == false) {
                    notificationPermissionState.launchPermissionRequest()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 2.dp
            ) {
                Column {
                    if (!searchActive) {
                        TopAppBar(
                            title = { Text(stringResource(R.string.screen_library)) },
                            actions = {
                                IconButton(onClick = { viewModel.onSearchActiveChange(true) }) {
                                    Icon(Icons.Default.Search, contentDescription = "Search")
                                }
                                IconButton(onClick = onNavigateToQueue) {
                                    Icon(Icons.Default.History, contentDescription = stringResource(R.string.tasks_list))
                                }
                                IconButton(onClick = onNavigateToSettings) {
                                    Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                                }
                            }
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.onSearchActiveChange(false) }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = null)
                            }
                            TextField(
                                value = searchQuery,
                                onValueChange = { viewModel.onSearchQueryChange(it) },
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                placeholder = { Text("動画を検索...") },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent
                                ),
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                            Icon(Icons.Default.Close, contentDescription = null)
                                        }
                                    }
                                },
                                singleLine = true
                            )
                        }
                    }
                    
                    if (searchActive && suggestions.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(suggestions.size) { index ->
                                val tag = suggestions[index]
                                AssistChip(
                                    onClick = { viewModel.onSearchQueryChange(tag) },
                                    label = { Text(tag) }
                                )
                            }
                        }
                    }

                    if (!searchActive) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val sortLabel = when (sortOrder) {
                                0 -> stringResource(R.string.sort_date_newest)
                                1 -> stringResource(R.string.sort_date_oldest)
                                2 -> stringResource(R.string.sort_size_largest)
                                3 -> stringResource(R.string.sort_size_smallest)
                                4 -> stringResource(R.string.sort_name_az)
                                else -> ""
                            }
                            AssistChip(
                                onClick = { showSortMenu = true },
                                label = { Text(sortLabel, color = MaterialTheme.colorScheme.onSecondaryContainer) },
                                leadingIcon = { Icon(Icons.Default.Sort, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                border = null
                            )
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.sort_date_newest)) }, onClick = { viewModel.setSortOrder(0); showSortMenu = false })
                                DropdownMenuItem(text = { Text(stringResource(R.string.sort_date_oldest)) }, onClick = { viewModel.setSortOrder(1); showSortMenu = false })
                                DropdownMenuItem(text = { Text(stringResource(R.string.sort_size_largest)) }, onClick = { viewModel.setSortOrder(2); showSortMenu = false })
                                DropdownMenuItem(text = { Text(stringResource(R.string.sort_size_smallest)) }, onClick = { viewModel.setSortOrder(3); showSortMenu = false })
                                DropdownMenuItem(text = { Text(stringResource(R.string.sort_name_az)) }, onClick = { viewModel.setSortOrder(4); showSortMenu = false })
                            }

                            IconButton(
                                onClick = { viewModel.toggleViewMode() },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Icon(
                                    if (isGridView) Icons.Default.ViewList else Icons.Default.ViewModule, 
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedVideos.isNotEmpty()) {
                FloatingActionButton(onClick = {
                    val selectedUris = videos.filter { selectedVideos.contains(it.id) }.map { it.uri.toString() }
                    if (selectedUris.isNotEmpty()) {
                        val json = "[\"" + selectedUris.joinToString("\",\"") + "\"]"
                        val encoded = android.util.Base64.encodeToString(json.toByteArray(), android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
                        onNavigateToPreview(encoded)
                    }
                }) { 
                    Text(stringResource(R.string.next_selected, selectedVideos.count()), modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    ) { innerPadding ->
        if (permissionState.status.isGranted) {
            if (isLoading && videos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                if (videos.isEmpty() && !isLoading) {
                    EmptyLibraryView(innerPadding) { viewModel.loadVideos(reset = true) }
                } else {
                    if (isGridView) {
                        VideoGrid(
                            state = gridState,
                            videos = videos,
                            selectedIds = selectedVideos,
                            showResolution = showResolution,
                            onClick = { video -> 
                                scope.launch { viewModel.toggleSelection(video) } 
                            },
                            onLongClick = { video ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                previewVideo = video
                            },
                            onLoadMore = { viewModel.loadVideos(reset = false) },
                            contentPadding = innerPadding
                        )
                    } else {
                        VideoList(
                            state = listState,
                            videos = videos,
                            selectedIds = selectedVideos,
                            showResolution = showResolution,
                            onClick = { video -> 
                                scope.launch { viewModel.toggleSelection(video) } 
                            },
                            onLongClick = { video ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                previewVideo = video
                            },
                            onLoadMore = { viewModel.loadVideos(reset = false) },
                            contentPadding = innerPadding
                        )
                    }
                }
            }
        } else {
            PermissionDeniedView(innerPadding) { permissionState.launchPermissionRequest() }
        }
    }

    // Video Preview Dialog
    previewVideo?.let { video ->
        LibraryPreviewDialog(
            video = video,
            onDismiss = { previewVideo = null }
        )
    }
}

@Composable
fun LibraryPreviewDialog(
    video: VideoItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(video.uri))
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
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = Color.White)
                    }
                    Text(
                        video.name,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                }

                // Player
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = true
                                // グレーのオーバーレイ（Shutter）を消す
                                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .offset(y = 24.dp) // シークバー（画面全体）を少し下に下げる
                    )
                }

                // Info footer
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp).navigationBarsPadding()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("サイズ: ${video.sizeFormatted}", style = MaterialTheme.typography.bodyMedium)
                                Text("長さ: ${video.durationFormatted}", style = MaterialTheme.typography.bodyMedium)
                            }
                            Text(video.resolutionLabel, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.close))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoGrid(
    state: LazyGridState,
    videos: List<VideoItem>,
    selectedIds: Set<Long>,
    showResolution: Boolean,
    onClick: (VideoItem) -> Unit,
    onLongClick: (VideoItem) -> Unit,
    onLoadMore: () -> Unit,
    contentPadding: PaddingValues
) {
    val reachedBottom by remember {
        derivedStateOf {
            val lastVisible = state.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisible != null && lastVisible.index == state.layoutInfo.totalItemsCount - 1
        }
    }
    LaunchedEffect(reachedBottom) { if (reachedBottom) onLoadMore() }

    LazyVerticalGrid(
        state = state,
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(
            start = 12.dp, end = 12.dp, top = contentPadding.calculateTopPadding() + 8.dp, bottom = 80.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(items = videos, key = { it.id }) { video ->
            VideoGridItem(
                video = video,
                isSelected = selectedIds.contains(video.id),
                showResolution = showResolution,
                onClick = { onClick(video) },
                onLongClick = { onLongClick(video) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoGridItem(
    video: VideoItem,
    isSelected: Boolean,
    showResolution: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                VideoThumbnail(uri = video.uri, modifier = Modifier.fillMaxSize())
                
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(video.durationFormatted, color = Color.White, style = MaterialTheme.typography.labelSmall)
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(40.dp))
                    }
                }
            }
        }

        Column(
            modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp, bottom = 4.dp)
        ) {
            Text(
                text = video.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = video.sizeFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (showResolution) {
                    Text(
                        text = video.resolutionLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun VideoList(
    state: LazyListState,
    videos: List<VideoItem>,
    selectedIds: Set<Long>,
    showResolution: Boolean,
    onClick: (VideoItem) -> Unit,
    onLongClick: (VideoItem) -> Unit,
    onLoadMore: () -> Unit,
    contentPadding: PaddingValues
) {
    val reachedBottom by remember {
        derivedStateOf {
            val lastVisible = state.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisible != null && lastVisible.index == state.layoutInfo.totalItemsCount - 1
        }
    }
    LaunchedEffect(reachedBottom) { if (reachedBottom) onLoadMore() }

    LazyColumn(
        state = state,
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 8.dp, bottom = 80.dp
        ),
        modifier = Modifier.fillMaxSize()
    ) {
        items(items = videos, key = { it.id }) { video ->
            VideoListItem(
                video = video,
                isSelected = selectedIds.contains(video.id),
                showResolution = showResolution,
                onClick = { onClick(video) },
                onLongClick = { onLongClick(video) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoListItem(
    video: VideoItem,
    isSelected: Boolean,
    showResolution: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            modifier = Modifier.width(120.dp).aspectRatio(16f / 9f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                VideoThumbnail(uri = video.uri, modifier = Modifier.fillMaxSize())
                if (isSelected) {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(video.name, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(video.sizeFormatted, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(8.dp))
                Text(video.durationFormatted, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (showResolution) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(video.resolutionLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun EmptyLibraryView(padding: PaddingValues, onRefresh: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Outlined.Videocam, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.surfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.no_videos_found), style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onRefresh) { Text(stringResource(R.string.refresh)) }
    }
}

@Composable
fun PermissionDeniedView(padding: PaddingValues, onRequest: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.VideoLibrary, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.access_library_needed), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRequest) { Text(stringResource(R.string.grant_permission)) }
        }
    }
}