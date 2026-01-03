package com.example.uiedvideocompacter.data.manager

import android.content.Context
import android.net.Uri
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import com.example.uiedvideocompacter.data.model.CompressionPreset
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File

import android.util.Log

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@UnstableApi
class CompressionEngine(private val context: Context) {

    fun compress(
        inputUri: Uri,
        outputFile: File,
        preset: CompressionPreset,
        useHevc: Boolean = false
    ): Flow<CompressionStatus> = callbackFlow {
        val maskedUri = inputUri.toString().take(30) + "..."
        val maskedPath = outputFile.name
        Log.d("CompressionEngine", "Starting compression for $maskedUri to $maskedPath (HEVC: $useHevc)")

        // Declare variables visible to awaitClose
        var transformer: Transformer? = null
        val handler = Handler(Looper.getMainLooper())
        var progressRunnable: Runnable? = null
        var isCancelled = false
        var isCompleted = false

        try {
            // Disk I/O on IO thread
            withContext(Dispatchers.IO) {
                outputFile.parentFile?.mkdirs()
            }
            
            // Transformer initialization and start on Main thread
            withContext(Dispatchers.Main) {
                // Get source metadata to prevent size increase
                var sourceBitrate = 0
                try {
                    val retriever = android.media.MediaMetadataRetriever()
                    retriever.setDataSource(context, inputUri)
                    sourceBitrate = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toInt() ?: 0
                    retriever.release()
                } catch (e: Exception) {
                    Log.w("CompressionEngine", "Could not get source bitrate", e)
                }

                // Target bitrate: lower of (preset) or (source * 0.8)
                val targetBitrate = if (sourceBitrate > 0) {
                    minOf(preset.bitrate, (sourceBitrate * 0.8).toInt())
                } else {
                    preset.bitrate
                }
                
                Log.d("CompressionEngine", "Source: $sourceBitrate bps -> Target: $targetBitrate bps")

                val mediaItem = MediaItem.fromUri(inputUri)

                // 解像度変更のエフェクトを作成
                val videoEffects = mutableListOf<Effect>()
                preset.height?.let { targetHeight ->
                    videoEffects.add(
                        Presentation.createForHeight(targetHeight)
                    )
                }

                val editedMediaItem = EditedMediaItem.Builder(mediaItem)
                    .setRemoveAudio(false)
                    .setRemoveVideo(false)
                    .setEffects(Effects(emptyList(), videoEffects))
                    .build()

                val encoderSettings = VideoEncoderSettings.Builder()
                    .setBitrate(targetBitrate)
                    .setBitrateMode(android.media.MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR)
                    .build()

                val encoderFactory = DefaultEncoderFactory.Builder(context)
                    .setRequestedVideoEncoderSettings(encoderSettings)
                    .setEnableFallback(true)
                    .build()

                val videoMimeType = if (useHevc) MimeTypes.VIDEO_H265 else MimeTypes.VIDEO_H264

                val transformerBuilder = Transformer.Builder(context)
                    .setVideoMimeType(videoMimeType)
                    .setAudioMimeType(MimeTypes.AUDIO_AAC)
                    .setEncoderFactory(encoderFactory)

                val newTransformer = transformerBuilder
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                            Log.d("CompressionEngine", "Compression completed")
                            isCompleted = true
                            trySend(CompressionStatus.Completed(outputFile))
                            close()
                        }

                        override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                            Log.e("CompressionEngine", "Compression error: ${exportException.javaClass.simpleName}")
                            trySend(CompressionStatus.Error(exportException))
                            close()
                        }
                    })
                    .build()
                
                transformer = newTransformer
                newTransformer.start(editedMediaItem, outputFile.absolutePath)

                // Start Polling
                val progressHolder = androidx.media3.transformer.ProgressHolder()
                
                progressRunnable = object : Runnable {
                    override fun run() {
                        if (isCancelled) return

                        val t = transformer ?: run {
                            progressRunnable = null
                            return
                        }
                        try {
                            val progressState = t.getProgress(progressHolder)
                            if (progressState == Transformer.PROGRESS_STATE_AVAILABLE) {
                                trySend(CompressionStatus.Progress(progressHolder.progress))
                            }
                            handler.postDelayed(this, 500)
                        } catch (e: Exception) {
                            progressRunnable = null
                        }
                    }
                }
                
                handler.post(progressRunnable!!)
            }

            // awaitClose must be outside withContext to suspend the flow correctly
            awaitClose {
                isCancelled = true
                progressRunnable?.let { handler.removeCallbacks(it) }
                val t = transformer
                if (t != null) {
                    handler.post {
                        try {
                            t.cancel()
                        } catch (ignore: Exception) {}
                    }
                }
                // Cleanup partial file if not completed
                if (!isCompleted && outputFile.exists()) {
                    try {
                        outputFile.delete()
                    } catch (e: Exception) {
                        Log.e("CompressionEngine", "Failed to cleanup partial file", e)
                    }
                }
            }
        } catch (e: Exception) {
            if (outputFile.exists()) outputFile.delete()
            Log.e("CompressionEngine", "Setup error", e)
            trySend(CompressionStatus.Error(e))
            close()
        }
    }
}

sealed class CompressionStatus {
    data class Progress(val progress: Int) : CompressionStatus()
    data class Completed(val outputFile: File) : CompressionStatus()
    data class Error(val exception: Exception) : CompressionStatus()
}
