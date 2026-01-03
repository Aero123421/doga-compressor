package com.example.uiedvideocompacter.data.model

import android.net.Uri

data class VideoItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val duration: Long,
    val size: Long,
    val dateAdded: Long,
    val width: Int,
    val height: Int,
    val mimeType: String,
    var isSelected: Boolean = false
) {
    val durationFormatted: String
        get() {
            val seconds = duration / 1000
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            val secs = seconds % 60
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, secs)
            } else {
                String.format("%d:%02d", minutes, secs)
            }
        }
    
    val resolutionLabel: String
        get() {
            val minDim = minOf(width, height)
            return if (minDim == 2160) "4K" else "${minDim}p"
        }
    
    val sizeFormatted: String
        get() {
            val sizeMb = size / (1024.0 * 1024.0)
            return String.format("%.2f MB", sizeMb)
        }
}
