package com.example.uiedvideocompacter.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.uiedvideocompacter.data.model.CompressionPreset
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

val Context.queueDataStore: DataStore<Preferences> by preferencesDataStore(name = "queue")

@Serializable
data class QueueItemData(
    val id: String,
    val name: String,
    val uri: String,
    val size: Long,
    val duration: Long,
    val presetName: String,
    val targetPercentage: Int? = null
)

class QueueStore(private val context: Context) {
    companion object {
        private val QUEUE_KEY = stringPreferencesKey("queue_items")
        private const val MAX_QUEUE_SIZE = 100
    }

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = false
        isLenient = false
    }

    suspend fun getQueue(): List<QueueItemData> {
        return try {
            val preferences = context.queueDataStore.data.first()
            val queueJson = preferences[QUEUE_KEY]
            if (queueJson != null) {
                json.decodeFromString<List<QueueItemData>>(queueJson)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            android.util.Log.e("QueueStore", "Failed to load queue", e)
            emptyList()
        }
    }

    suspend fun addToQueue(item: QueueItemData) {
        context.queueDataStore.edit { preferences ->
            try {
                val currentJson = preferences[QUEUE_KEY]
                val currentList = if (currentJson != null) {
                    json.decodeFromString<List<QueueItemData>>(currentJson)
                } else {
                    emptyList()
                }
                
                val existingIndex = currentList.indexOfFirst { it.uri == item.uri }
                val newList = if (existingIndex >= 0) {
                    val mutableList = currentList.toMutableList()
                    val existingItem = mutableList[existingIndex]
                    mutableList[existingIndex] = item.copy(id = existingItem.id) 
                    mutableList
                } else {
                    (currentList + item).takeLast(MAX_QUEUE_SIZE)
                }
                
                preferences[QUEUE_KEY] = json.encodeToString(newList)
            } catch (e: Exception) {
                android.util.Log.e("QueueStore", "Failed to add to queue", e)
            }
        }
    }

    suspend fun addAllToQueue(items: List<QueueItemData>) {
        context.queueDataStore.edit { preferences ->
            try {
                val currentJson = preferences[QUEUE_KEY]
                val currentList = if (currentJson != null) {
                    json.decodeFromString<List<QueueItemData>>(currentJson)
                } else {
                    emptyList()
                }
                
                val mutableList = currentList.toMutableList()
                
                items.forEach { newItem ->
                    val existingIndex = mutableList.indexOfFirst { it.uri == newItem.uri }
                    if (existingIndex >= 0) {
                        val existingItem = mutableList[existingIndex]
                        mutableList[existingIndex] = newItem.copy(id = existingItem.id)
                    } else {
                        mutableList.add(newItem)
                    }
                }
                
                val finalList = mutableList.takeLast(MAX_QUEUE_SIZE)
                preferences[QUEUE_KEY] = json.encodeToString(finalList)
            } catch (e: Exception) {
                android.util.Log.e("QueueStore", "Failed to add items to queue", e)
            }
        }
    }

    suspend fun removeFromQueue(id: String) {
        context.queueDataStore.edit { preferences ->
            try {
                val currentJson = preferences[QUEUE_KEY]
                if (currentJson != null) {
                    val currentList = json.decodeFromString<List<QueueItemData>>(currentJson)
                    val newList = currentList.filterNot { it.id == id }
                    preferences[QUEUE_KEY] = json.encodeToString(newList)
                }
            } catch (e: Exception) {
                android.util.Log.e("QueueStore", "Failed to remove from queue", e)
            }
        }
    }

    suspend fun clearQueue() {
        context.queueDataStore.edit { preferences ->
            preferences.remove(QUEUE_KEY)
        }
    }

    suspend fun updatePreset(id: String, preset: CompressionPreset) {
        context.queueDataStore.edit { preferences ->
            val currentJson = preferences[QUEUE_KEY]
            if (currentJson != null) {
                try {
                    val currentList = json.decodeFromString<List<QueueItemData>>(currentJson)
                    val newList = currentList.map { 
                        if (it.id == id) it.copy(presetName = preset.name) else it 
                    }
                    preferences[QUEUE_KEY] = json.encodeToString(newList)
                } catch (e: Exception) {
                    // Ignore or log
                }
            }
        }
    }

    suspend fun markCompressionStarted() {
        // Placeholder implementation
    }

    // Deprecated: Internal helper not needed if all methods use atomic edit
    // private suspend fun saveQueue(items: List<QueueItemData>) { ... }
}
