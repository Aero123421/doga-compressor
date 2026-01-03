package com.example.uiedvideocompacter.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis

@Composable
fun VideoThumbnail(
    uri: Any?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(uri)
            .decoderFactory { result, options, _ -> VideoFrameDecoder(result.source, options) }
            .videoFrameMillis(60000) // 1 minute in for better content visibility
            .crossfade(true)
            .build()
    )

    Box(modifier = modifier) {
        if (painter.state is AsyncImagePainter.State.Loading || painter.state is AsyncImagePainter.State.Error) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.DarkGray.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f)
                )
            }
        }
        
        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale
        )
    }
}
