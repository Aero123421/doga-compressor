package com.example.uiedvideocompacter.ui.screens.result

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.uiedvideocompacter.data.store.ResultStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class ResultItem(
    val id: String,
    val name: String,
    val originalUri: String?,
    val outputPath: String,
    val originalSize: Long,
    val compressedSize: Long,
    val isSuccess: Boolean,
    val errorMessage: String? = null,
    val originalDisplayName: String? = null,
    val originalRelativePath: String? = null,
    val originalVolumeName: String? = null
) {
    val originalSizeFormatted: String
        get() = "%.1f MB".format(originalSize / (1024.0 * 1024.0))

    val compressedSizeFormatted: String
        get() = "%.1f MB".format(compressedSize / (1024.0 * 1024.0))

    val savedPercent: String
        get() = if (originalSize > 0) {
            val percent = ((originalSize - compressedSize).toFloat() / originalSize * 100).toInt()
            "$percent%"
        } else {
            "0%"
        }
}

class ResultViewModel(application: Application) : AndroidViewModel(application) {
    private val resultStore = ResultStore(application)

    private val _results = MutableStateFlow<List<ResultItem>>(emptyList())
    val results: StateFlow<List<ResultItem>> = _results.asStateFlow()

    private val _deleteRequest = MutableStateFlow<PendingIntent?>(null)
    val deleteRequest: StateFlow<PendingIntent?> = _deleteRequest.asStateFlow()

    val successCount: StateFlow<Int> = _results
        .asStateFlow()
        .map { it.count { r -> r.isSuccess } }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val failCount: StateFlow<Int> = _results
        .asStateFlow()
        .map { it.count { r -> !r.isSuccess } }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    fun loadResults() {
        viewModelScope.launch {
            val resultDataList = resultStore.getResults()
            _results.value = resultDataList.map { data ->
                ResultItem(
                    id = data.id,
                    name = data.name,
                    originalUri = data.originalUri,
                    outputPath = data.outputPath,
                    originalSize = data.originalSize,
                    compressedSize = data.compressedSize,
                    isSuccess = data.isSuccess,
                    errorMessage = data.errorMessage,
                    originalDisplayName = data.originalDisplayName,
                    originalRelativePath = data.originalRelativePath,
                    originalVolumeName = data.originalVolumeName
                )
            }
        }
    }

    fun deleteResult(result: ResultItem) {
        viewModelScope.launch {
            // 中間ファイルも削除
            try {
                File(result.outputPath).delete()
            } catch (ignore: Exception) {}
            
            resultStore.deleteResult(result.id)
            loadResults()
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            // 全ての中間ファイルを削除
            _results.value.forEach { result ->
                try {
                    File(result.outputPath).delete()
                } catch (ignore: Exception) {}
            }
            resultStore.clearResults()
            loadResults()
        }
    }

    fun shareResult(result: ResultItem) {
        viewModelScope.launch {
            try {
                val file = File(result.outputPath)
                if (!file.exists()) return@launch
                
                val uri = FileProvider.getUriForFile(
                    getApplication(),
                    "${getApplication<Application>().packageName}.provider",
                    file
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "video/mp4"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                getApplication<Application>().startActivity(Intent.createChooser(intent, "共有").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun prepareReplaceOriginal(result: ResultItem) {
        val originalUriString = result.originalUri ?: return
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val uri = Uri.parse(originalUriString)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, listOf(uri))
                    _deleteRequest.value = pendingIntent
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun consumeDeleteRequest() {
        _deleteRequest.value = null
    }

    fun finalizeReplacement(result: ResultItem) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // 完全な情報を引き継いでギャラリーへ保存
                saveToGallery(result, isReplacement = true)
                // 成功したらリストから削除
                deleteResult(result)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun saveToGallery(result: ResultItem, isReplacement: Boolean = false) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val context = getApplication<Application>()
            val srcFile = File(result.outputPath)
            if (!srcFile.exists()) return@launch

            var itemUri: Uri? = null
            try {
                // Determine target filename: OriginalName_compressed.mp4 or exact original name for replacement
                val targetName = if (isReplacement) {
                    result.originalDisplayName ?: "video_${System.currentTimeMillis()}.mp4"
                } else {
                    val baseName = result.originalDisplayName?.substringBeforeLast(".") ?: "video_${System.currentTimeMillis()}"
                    "${baseName}_compressed.mp4"
                }
                
                val targetPath = if (isReplacement) {
                    result.originalRelativePath ?: "Movies/VideoCompacter"
                } else {
                    "Movies/VideoCompacter"
                }

                val volumeName = if (isReplacement) {
                    result.originalVolumeName ?: android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY
                } else {
                    android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY
                }

                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Video.Media.DISPLAY_NAME, targetName)
                    put(android.provider.MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        put(android.provider.MediaStore.Video.Media.RELATIVE_PATH, targetPath)
                        put(android.provider.MediaStore.Video.Media.IS_PENDING, 1)
                    }
                }

                val collection = android.provider.MediaStore.Video.Media.getContentUri(volumeName)
                itemUri = context.contentResolver.insert(collection, values)

                itemUri?.let { uri ->
                    context.contentResolver.openOutputStream(uri)?.use { outStream ->
                        srcFile.inputStream().use { inStream ->
                            inStream.copyTo(outStream)
                        }
                    }

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        values.clear()
                        values.put(android.provider.MediaStore.Video.Media.IS_PENDING, 0)
                        context.contentResolver.update(uri, values, null, null)
                    }
                    
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "ギャラリーに保存しました", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                itemUri?.let { uri -> try { context.contentResolver.delete(uri, null, null) } catch (ignore: Exception) {} }
            }
        }
    }
}
