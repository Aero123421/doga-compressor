package com.example.uiedvideocompacter.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import com.example.uiedvideocompacter.data.model.VideoItem

class VideoRepository(private val context: Context) {

    fun getVideos(limit: Int, offset: Int): Result<List<VideoItem>> {
        val videoList = mutableListOf<VideoItem>()
        val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.MIME_TYPE
        )

        // Android 10+ (API 29+) query arguments
        val queryArgs = Bundle().apply {
            putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
            putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
            putStringArray(
                ContentResolver.QUERY_ARG_SORT_COLUMNS,
                arrayOf(MediaStore.Video.Media.DATE_ADDED)
            )
            putInt(
                ContentResolver.QUERY_ARG_SORT_DIRECTION,
                ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
            )
        }

        return try {
            val query = context.contentResolver.query(
                collection,
                projection,
                queryArgs,
                null
            )

            query?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Unknown"
                    val duration = cursor.getLong(durationColumn)
                    val size = cursor.getLong(sizeColumn)
                    val dateAdded = cursor.getLong(dateAddedColumn)
                    val width = cursor.getInt(widthColumn)
                    val height = cursor.getInt(heightColumn)
                    val mimeType = cursor.getString(mimeTypeColumn) ?: "video/*"

                    val contentUri = ContentUris.withAppendedId(collection, id)

                    // Fallback for size 0 or negative
                    var finalSize = size
                    if (finalSize <= 0) {
                        try {
                            context.contentResolver.openFileDescriptor(contentUri, "r")?.use { pfd ->
                                finalSize = pfd.statSize
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("VideoRepository", "Failed to get file size for id $id", e)
                            finalSize = 0
                        }
                    }
                    if (finalSize < 0) {
                        finalSize = 0
                    }

                    videoList.add(
                        VideoItem(
                            id = id,
                            uri = contentUri,
                            name = name,
                            duration = duration,
                            size = finalSize,
                            dateAdded = dateAdded,
                            width = width,
                            height = height,
                            mimeType = mimeType
                        )
                    )
                }
            }
            Result.success(videoList)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
