package com.example.uiedvideocompacter.data.manager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.uiedvideocompacter.data.model.CompressionPreset
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import java.io.File

class CompressionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val KEY_INPUT_URI = "input_uri"
        const val KEY_OUTPUT_PATH = "output_path"
        const val KEY_PRESET_NAME = "preset_name"
        const val KEY_ORIGINAL_SIZE = "original_size"
        const val KEY_ORIGINAL_NAME = "original_name"

        const val CHANNEL_ID = "compression_channel"
        const val NOTIFICATION_ID = 1

        private const val STORAGE_MULTIPLIER = 1.5
        private const val MIN_REQUIRED_SPACE_BYTES = 50L * 1024 * 1024
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo(0)
    }

    override suspend fun doWork(): Result {
        val inputUriString = inputData.getString(KEY_INPUT_URI) ?: return Result.failure()
        val inputUri = try {
            val uri = Uri.parse(inputUriString)
            val scheme = uri.scheme
            if (scheme == null || (scheme != "content" && scheme != "file")) {
                throw IllegalArgumentException("Invalid URI scheme")
            }
            uri
        } catch (e: Exception) {
            return Result.failure(workDataOf("error" to "Invalid input URI"))
        }

        val presetName = inputData.getString(KEY_PRESET_NAME) ?: CompressionPreset.BALANCED.name
        val preset = try {
            CompressionPreset.valueOf(presetName)
        } catch (e: Exception) {
            CompressionPreset.BALANCED
        }
        
        val targetPercentageRaw = inputData.getInt("target_percentage", -1)
        val targetPercentage = if (targetPercentageRaw != -1) targetPercentageRaw else null

        val originalSize = inputData.getLong(KEY_ORIGINAL_SIZE, 0L)
        val originalName = inputData.getString(KEY_ORIGINAL_NAME) ?: applicationContext.getString(com.example.uiedvideocompacter.R.string.app_name)

        val outputName = "compressed_${System.currentTimeMillis()}.mp4"
        val outputDir = applicationContext.getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES)
            ?: applicationContext.filesDir
        val outputFile = File(outputDir, outputName)

        if (!outputFile.absolutePath.startsWith(outputDir.absolutePath)) {
            return Result.failure(workDataOf("error" to "Invalid output file path"))
        }

        // Storage check: Require at least 1.5x original size or 50MB free
        val freeSpace = outputDir.usableSpace
        val requiredSpace = ((originalSize * STORAGE_MULTIPLIER).toLong()).coerceAtLeast(MIN_REQUIRED_SPACE_BYTES)
        if (freeSpace < requiredSpace) {
            return Result.failure(workDataOf("error" to "Insufficient storage space"))
        }

        createNotificationChannel()
        setForeground(createForegroundInfo(0))

        val engine = CompressionEngine(applicationContext)
        val resultStore = com.example.uiedvideocompacter.data.store.ResultStore(applicationContext)
        val userPreferences = com.example.uiedvideocompacter.data.store.UserPreferences(applicationContext)
        val useHevc = userPreferences.useHevc.first()
        
        var finalResult = Result.failure()

        try {
            engine.compress(inputUri, outputFile, preset, useHevc, targetPercentage).collect { status ->
                when (status) {
                    is CompressionStatus.Progress -> {
                        setForeground(createForegroundInfo(status.progress))
                        setProgress(workDataOf("progress" to status.progress))
                    }
                    is CompressionStatus.Completed -> {
                        // Extract metadata for perfect replacement
                        var origDisplayName: String? = null
                        var origRelativePath: String? = null
                        var origVolume: String? = null
                        
                        try {
                            val projection = arrayOf(
                                android.provider.MediaStore.Video.Media.DISPLAY_NAME,
                                android.provider.MediaStore.Video.Media.RELATIVE_PATH,
                                android.provider.MediaStore.Video.Media.VOLUME_NAME
                            )
                            applicationContext.contentResolver.query(inputUri, projection, null, null, null)?.use { cursor ->
                                if (cursor.moveToFirst()) {
                                    origDisplayName = cursor.getString(0)
                                    origRelativePath = cursor.getString(1)
                                    origVolume = cursor.getString(2)
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("CompressionWorker", "Failed to query metadata", e)
                        }

                        val resultItem = com.example.uiedvideocompacter.data.store.ResultItemData(
                            id = java.util.UUID.randomUUID().toString(),
                            name = originalName,
                            originalUri = inputUriString,
                            outputPath = status.outputFile.absolutePath,
                            originalSize = originalSize,
                            compressedSize = status.outputFile.length(),
                            isSuccess = true,
                            originalDisplayName = origDisplayName,
                            originalRelativePath = origRelativePath,
                            originalVolumeName = origVolume
                        )
                        resultStore.addResult(resultItem)
                        
                        finalResult = Result.success(workDataOf(KEY_OUTPUT_PATH to status.outputFile.absolutePath))
                    }
                    is CompressionStatus.Error -> {
                        if (outputFile.exists()) outputFile.delete()
                        
                        val resultItem = com.example.uiedvideocompacter.data.store.ResultItemData(
                            id = java.util.UUID.randomUUID().toString(),
                            name = originalName,
                            originalUri = inputUriString,
                            outputPath = "",
                            originalSize = originalSize,
                            compressedSize = 0,
                            isSuccess = false,
                            errorMessage = status.exception.message
                        )
                        resultStore.addResult(resultItem)

                        finalResult = Result.failure(workDataOf("error" to (status.exception.message ?: "Unknown compression error")))
                    }
                }
            }
        } catch (e: Exception) {
            if (outputFile.exists()) outputFile.delete()
            finalResult = Result.failure(workDataOf("error" to (e.message ?: "Worker crashed")))
        }
        
        return finalResult
    }

    private fun createForegroundInfo(progress: Int): ForegroundInfo {
        val title = applicationContext.getString(com.example.uiedvideocompacter.R.string.notification_title)
        
        val icon = android.R.drawable.stat_sys_download 

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setTicker(title)
            .setContentText("$progress%")
            .setSmallIcon(icon)
            .setOngoing(true)
            .setProgress(100, progress, false)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                applicationContext.getString(com.example.uiedvideocompacter.R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }
}
