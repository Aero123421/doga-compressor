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
        useHevc: Boolean = false,
        targetPercentage: Int? = null
    ): Flow<CompressionStatus> = callbackFlow {
        val maskedUri = inputUri.toString().take(30) + "..."
        val maskedPath = outputFile.name
        Log.d("CompressionEngine", "Starting compression for $maskedUri to $maskedPath (HEVC: $useHevc, Target%: $targetPercentage)")

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
                // Get source metadata
                var sourceBitrate = 0
                var durationMs = 0L
                var width = 1920
                var height = 1080
                
                try {
                    val retriever = android.media.MediaMetadataRetriever()
                    retriever.setDataSource(context, inputUri)
                    sourceBitrate = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toInt() ?: 0
                    durationMs = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
                    width = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 1920
                    height = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 1080
                    retriever.release()
                } catch (e: Exception) {
                    Log.w("CompressionEngine", "Could not get source metadata", e)
                }

                // Calculate Target Bitrate and Resolution
                var finalTargetBitrate = 0
                var finalTargetHeight = preset.height
                
                if (targetPercentage != null && durationMs > 0) {
                    var originalSize = 0L
                    try {
                        context.contentResolver.openFileDescriptor(inputUri, "r")?.use { pfd ->
                            originalSize = pfd.statSize
                        }
                    } catch (e: Exception) {
                        Log.w("CompressionEngine", "Could not get file size", e)
                    }

                    if (originalSize > 0) {
                        // Calculate target bits: (OriginalBytes * Percentage/100) * 8
                        val targetSizeBits = (originalSize * (targetPercentage / 100.0) * 8).toLong()
                        val durationSec = durationMs / 1000.0
                        val calculatedBitrate = (targetSizeBits / durationSec).toInt()
                        
                        finalTargetBitrate = calculatedBitrate
                        
                        // Adaptive Resolution Safety Check
                        // Bits Per Pixel (BPP) = Bitrate / (Width * Height * FPS)
                        // FPS assumed 30 for safety estimation
                        val pixelCount = width * height
                        if (pixelCount > 0) {
                            val bpp = calculatedBitrate.toDouble() / (pixelCount * 30)
                            Log.d("CompressionEngine", "Adaptive Check: Bitrate=$calculatedBitrate, BPP=$bpp")
                            
                            // If BPP is too low (below 0.05), quality will be blocky. Downscale.
                            if (bpp < 0.05) {
                                if (height > 1080) finalTargetHeight = 1080
                                else if (height > 720) finalTargetHeight = 720
                                else if (height > 480) finalTargetHeight = 480
                                Log.d("CompressionEngine", "BPP too low, downscaling to ${finalTargetHeight}p")
                            } else {
                                finalTargetHeight = null // Keep original
                            }
                        }
                    }
                }

                // Fallback if adaptive failed or not requested
                if (finalTargetBitrate == 0) {
                    finalTargetBitrate = if (sourceBitrate > 0) {
                        minOf(preset.bitrate, (sourceBitrate * 0.8).toInt())
                    } else {
                        preset.bitrate
                    }
                }
                
                Log.d("CompressionEngine", "Source: $sourceBitrate bps -> Target: $finalTargetBitrate bps, Height: $finalTargetHeight")

                val mediaItem = MediaItem.fromUri(inputUri)

                // Create Effects
                val videoEffects = mutableListOf<Effect>()
                finalTargetHeight?.let { targetH ->
                    // Only apply if target is smaller than source to avoid upscaling
                    if (targetH < height) {
                         videoEffects.add(Presentation.createForHeight(targetH))
                    }
                }

                val editedMediaItem = EditedMediaItem.Builder(mediaItem)
                    .setRemoveAudio(false)
                    .setRemoveVideo(false)
                    .setEffects(Effects(emptyList(), videoEffects))
                    .build()

                val encoderSettings = VideoEncoderSettings.Builder()
                    .setBitrate(finalTargetBitrate)
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
