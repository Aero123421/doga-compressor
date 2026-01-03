package com.example.uiedvideocompacter.ui.screens.preview

import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.ui.PlayerView
import com.example.uiedvideocompacter.R
import com.example.uiedvideocompacter.data.model.CompressionPreset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    uriString: String?,
    viewModel: PreviewViewModel = viewModel(),
    onNavigateToQueue: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    
    // Decode URIs from JSON array string
    val uris = remember(uriString) {
        if (uriString.isNullOrEmpty()) emptyList<Uri>()
        else {
            try {
                val decoded = String(android.util.Base64.decode(uriString, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP))
                // Simple manual parse for ["uri1","uri2"]
                decoded.trim('[', ']')
                    .split(",\"")
                    .map { it.trim('"') }
                    .filter { it.isNotEmpty() }
                    .map { Uri.parse(it) }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList<Uri>()
            }
        }
    }
    
    if (uris.isEmpty()) {
        LaunchedEffect(Unit) {
            onBack()
        }
        return
    }
    
    LaunchedEffect(uris) {
        viewModel.setUris(uris)
        viewModel.setCompressionPercentage(50)
    }
    
    DisposableEffect(Unit) {
        onDispose { viewModel.releasePlayer() }
    }

    val pagerState = rememberPagerState(pageCount = { uris.size })
    
    LaunchedEffect(pagerState.currentPage) {
        viewModel.onPageChanged(pagerState.currentPage)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(stringResource(R.string.screen_preview))
                        if (uris.size > 1) {
                            Text(
                                "${pagerState.currentPage + 1} / ${uris.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
             Button(
                onClick = {
                    viewModel.addToQueue { success ->
                        if (success) {
                            onNavigateToQueue()
                        } else {
                            Toast.makeText(context, context.getString(R.string.already_in_queue), Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                 val text = if (uris.size > 1) {
                     "全${uris.size}個をキューに追加"
                 } else {
                     stringResource(R.string.add_to_queue)
                 }
                 Text(text, style = MaterialTheme.typography.titleMedium)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            
            // Video Player Area with Pager
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val player = if (pagerState.currentPage == page) viewModel.player else null
                    
                    if (player != null) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    this.player = player
                                    useController = true
                                    controllerShowTimeoutMs = 2000
                                }
                            },
                            update = { view -> view.player = player },
                            modifier = Modifier.fillMaxSize().background(Color.Black)
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Stats Card (Applies to all)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.estimated_size, viewModel.estimatedSize),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                "圧縮設定",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp),
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                 modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("目標サイズ: ${viewModel.compressionPercentage}%")
                        Text("${100 - viewModel.compressionPercentage}% 削減")
                    }

                    androidx.compose.material3.Slider(
                        value = viewModel.compressionPercentage.toFloat(),
                        onValueChange = { viewModel.setCompressionPercentage(it.toInt()) },
                        valueRange = 10f..100f,
                        steps = 17, // (100-10)/5 - 1 = 17 steps for 5% increments roughly
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (viewModel.compressionPercentage < 30) {
                         Text(
                            "※ 設定値が低すぎるため、画質が大幅に低下するか、自動的に解像度が下がる可能性があります。",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else if (viewModel.compressionPercentage > 90) {
                        Text(
                            "※ ほとんど圧縮されません。",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}