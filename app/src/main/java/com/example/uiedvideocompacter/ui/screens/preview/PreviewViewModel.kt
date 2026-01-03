package com.example.uiedvideocompacter.ui.screens.preview

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.uiedvideocompacter.data.model.CompressionPreset
import com.example.uiedvideocompacter.data.store.QueueItemData
import com.example.uiedvideocompacter.data.store.QueueStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class PreviewViewModel(application: Application) : AndroidViewModel(application) {
    
    private val queueStore = QueueStore(application)
    
    var player by mutableStateOf<ExoPlayer?>(null)
        private set
        
    var compressionPercentage by mutableStateOf(50)
        private set
        
    var estimatedSize by mutableStateOf("Calculating...")
        private set
        
    var videoUris by mutableStateOf<List<Uri>>(emptyList())
        private set

    fun setUris(uris: List<Uri>) {
        videoUris = uris
        if (uris.isNotEmpty()) {
            // Initial player for the first video
            initializePlayer(uris[0])
            updateEstimation()
        }
    }

    fun onPageChanged(index: Int) {
        if (index in videoUris.indices) {
            initializePlayer(videoUris[index])
        }
    }

    private fun initializePlayer(uri: Uri) {
        releasePlayer()
        player = ExoPlayer.Builder(getApplication()).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            volume = 0f // Completely mute the app
            prepare()
            playWhenReady = false
        }
    }

    fun releasePlayer() {
        player?.release()
        player = null
    }

    fun setCompressionPercentage(percentage: Int) {
        compressionPercentage = percentage
        updateEstimation()
    }

    private fun updateEstimation() {
        viewModelScope.launch(Dispatchers.IO) {
            var totalDurationMs = 0L
            var totalOriginalSizeBytes = 0L
            
            videoUris.forEach { uri ->
                try {
                    val retriever = android.media.MediaMetadataRetriever()
                    retriever.setDataSource(getApplication(), uri)
                    val duration = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
                    totalDurationMs += duration
                    retriever.release()
                    
                    getApplication<Application>().contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        totalOriginalSizeBytes += pfd.statSize
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            withContext(Dispatchers.Main) {
                if (totalOriginalSizeBytes > 0) {
                    val finalEstSize = (totalOriginalSizeBytes * (compressionPercentage / 100.0)).toLong()
                    
                    val sizeMb = finalEstSize / (1024.0 * 1024.0)
                    val originalMb = totalOriginalSizeBytes / (1024.0 * 1024.0)
                    val savingPercent = 100 - compressionPercentage

                    estimatedSize = String.format("~%.1f MB (元: %.1f MB / -%d%%)", sizeMb, originalMb, savingPercent)
                } else {
                    estimatedSize = "集計中..."
                }
            }
        }
    }
    
    fun addToQueue(onComplete: (Boolean) -> Unit) {
        if (videoUris.isEmpty()) return
        
        viewModelScope.launch {
            val items = videoUris.map { uri ->
                // Get original filename and metadata
                var name = "Video_${System.currentTimeMillis()}"
                var size = 0L
                var duration = 0L
                
                try {
                    val projection = arrayOf(android.provider.MediaStore.Video.Media.DISPLAY_NAME)
                    getApplication<Application>().contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            name = cursor.getString(0) ?: name
                        }
                    }

                    getApplication<Application>().contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        size = pfd.statSize
                    }
                    
                    val retriever = android.media.MediaMetadataRetriever()
                    retriever.setDataSource(getApplication(), uri)
                    duration = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
                    retriever.release()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                QueueItemData(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    uri = uri.toString(),
                    size = size,
                    duration = duration,
                    presetName = "CUSTOM",
                    compressionPercentage = compressionPercentage
                )
            }
            queueStore.addAllToQueue(items)
            onComplete(true)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }
}
