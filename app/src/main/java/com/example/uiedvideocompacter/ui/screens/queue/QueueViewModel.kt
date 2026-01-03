package com.example.uiedvideocompacter.ui.screens.queue

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.uiedvideocompacter.data.manager.CompressionWorker
import com.example.uiedvideocompacter.data.model.CompressionPreset
import com.example.uiedvideocompacter.data.store.QueueItemData
import com.example.uiedvideocompacter.data.store.QueueStore
import com.example.uiedvideocompacter.data.store.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class QueueItem(
    val id: String,
    val name: String,
    val uri: String,
    val size: Long,
    val duration: Long,
    val compressionPercentage: Int
) {
    val sizeFormatted: String
        get() = "%.1f MB".format(size / (1024.0 * 1024.0))

    val durationFormatted: String
        get() {
            val seconds = duration / 1000
            val minutes = seconds / 60
            val secs = seconds % 60
            return if (minutes > 0) "${minutes}m ${secs}s" else "${secs}s"
        }

    val estimatedSizeFormatted: String
        get() = "%.1f MB".format((size * (compressionPercentage / 100.0)) / (1024.0 * 1024.0))
}

class QueueViewModel(application: Application) : AndroidViewModel(application) {
    private val queueStore = QueueStore(application)
    private val userPreferences = UserPreferences(application)

    private val _queueItems = MutableStateFlow<List<QueueItem>>(emptyList())
    val queueItems: StateFlow<List<QueueItem>> = _queueItems.asStateFlow()

    private val _totalSize = MutableStateFlow<String>("")
    val totalSize: StateFlow<String> = _totalSize.asStateFlow()

    private val _estimatedSize = MutableStateFlow<String>("")
    val estimatedSize: StateFlow<String> = _estimatedSize.asStateFlow()

    private val _savings = MutableStateFlow<String>("")
    val savings: StateFlow<String> = _savings.asStateFlow()

    fun loadQueue() {
        viewModelScope.launch {
            val itemsData = queueStore.getQueue()
            val items = itemsData.map { data ->
                QueueItem(
                    id = data.id,
                    name = data.name,
                    uri = data.uri,
                    size = data.size,
                    duration = data.duration,
                    compressionPercentage = data.compressionPercentage
                )
            }
            _queueItems.value = items
            calculateStats(items)
        }
    }

    fun removeFromQueue(id: String) {
        viewModelScope.launch {
            queueStore.removeFromQueue(id)
            loadQueue()
        }
    }

    fun updatePreset(id: String, percentage: Int) {
        viewModelScope.launch {
            queueStore.updateCompressionPercentage(id, percentage)
            loadQueue()
        }
    }

    fun startCompression(onNavigateToProgress: () -> Unit) {
        viewModelScope.launch {
            val itemsData = queueStore.getQueue()
            if (itemsData.isNotEmpty()) {
                val maxParallel = userPreferences.maxParallelTasks.first().coerceIn(1, 3)
                
                val items = itemsData.map { data ->
                    QueueItem(
                        id = data.id,
                        name = data.name,
                        uri = data.uri,
                        size = data.size,
                        duration = data.duration,
                        compressionPercentage = data.compressionPercentage
                    )
                }
                val workManager = WorkManager.getInstance(getApplication())
                
                // Prune old finished/failed jobs to keep the progress view clean
                workManager.pruneWork()

                // Split items into groups for parallel chains
                // If items=5 and maxParallel=2 -> group1=[0,2,4], group2=[1,3]
                val groups = List(maxParallel) { mutableListOf<QueueItem>() }
                items.forEachIndexed { index, item ->
                    groups[index % maxParallel].add(item)
                }

                groups.forEach { group ->
                    if (group.isNotEmpty()) {
                        var workContinuation = workManager.beginWith(createWorkRequest(group.first()))
                        for (i in 1 until group.size) {
                            workContinuation = workContinuation.then(createWorkRequest(group[i]))
                        }
                        workContinuation.enqueue()
                    }
                }
                
                queueStore.clearQueue() // Clear queue after handing over to WorkManager
                onNavigateToProgress()
            }
        }
    }

    private fun createWorkRequest(item: QueueItem): OneTimeWorkRequest {
        return OneTimeWorkRequestBuilder<CompressionWorker>()
            .setInputData(
                workDataOf(
                    CompressionWorker.KEY_INPUT_URI to item.uri,
                    CompressionWorker.KEY_COMPRESSION_PERCENTAGE to item.compressionPercentage,
                    CompressionWorker.KEY_ORIGINAL_SIZE to item.size,
                    CompressionWorker.KEY_ORIGINAL_NAME to item.name,
                    CompressionWorker.KEY_DURATION to item.duration,
                    "item_id" to item.id
                )
            )
            .addTag("compression")
            .addTag("compression_${item.id}")
            .setBackoffCriteria(
                androidx.work.BackoffPolicy.LINEAR,
                30,
                java.util.concurrent.TimeUnit.SECONDS
            )
            .build()
    }

    private fun calculateStats(items: List<QueueItem>) {
        val totalBytes = items.sumOf { it.size }
        val estimatedBytes = items.sumOf { (it.size * (it.compressionPercentage / 100.0)).toLong() }

        _totalSize.value = "%.1f MB".format(totalBytes / (1024.0 * 1024.0))
        _estimatedSize.value = "%.1f MB".format(estimatedBytes / (1024.0 * 1024.0))

        if (totalBytes > 0) {
            val savedPercent = ((totalBytes - estimatedBytes).toFloat() / totalBytes * 100).toInt()
            _savings.value = "$savedPercent%"
        } else {
            _savings.value = "0%"
        }
    }
}
