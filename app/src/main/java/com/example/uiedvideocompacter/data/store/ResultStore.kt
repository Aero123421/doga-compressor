package com.example.uiedvideocompacter.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

val Context.resultDataStore: DataStore<Preferences> by preferencesDataStore(name = "results")

@Serializable
data class ResultItemData(
    val id: String,
    val name: String,
    val originalUri: String? = null,
    val outputPath: String,
    val originalSize: Long,
    val compressedSize: Long,
    val isSuccess: Boolean,
    val errorMessage: String? = null,
    val originalDisplayName: String? = null,
    val originalRelativePath: String? = null,
    val originalVolumeName: String? = null
)

class ResultStore(private val context: Context) {
    companion object {
        private val RESULTS_KEY = stringPreferencesKey("results")
        private const val MAX_RESULTS = 50
    }

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = false
        isLenient = false
    }

    suspend fun getResults(): List<ResultItemData> {
        return try {
            val preferences = context.resultDataStore.data.first()
            val resultsJson = preferences[RESULTS_KEY]
            if (resultsJson != null) {
                json.decodeFromString<List<ResultItemData>>(resultsJson)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            android.util.Log.e("ResultStore", "Failed to load results", e)
            emptyList()
        }
    }

    suspend fun addResult(item: ResultItemData) {
        try {
            val currentList = getResults()
            val newList = (listOf(item) + currentList).take(MAX_RESULTS)
            context.resultDataStore.edit { preferences ->
                preferences[RESULTS_KEY] = json.encodeToString(newList)
            }
        } catch (e: Exception) {
            android.util.Log.e("ResultStore", "Failed to add result", e)
        }
    }

    suspend fun deleteResult(id: String) {
        context.resultDataStore.edit { preferences ->
            try {
                val currentJson = preferences[RESULTS_KEY]
                if (currentJson != null) {
                    val currentList = json.decodeFromString<List<ResultItemData>>(currentJson)
                    val newList = currentList.filterNot { it.id == id }
                    preferences[RESULTS_KEY] = json.encodeToString(newList)
                }
            } catch (e: Exception) {
                android.util.Log.e("ResultStore", "Failed to delete result", e)
            }
        }
    }

    suspend fun clearResults() {
        context.resultDataStore.edit { preferences ->
            preferences.remove(RESULTS_KEY)
        }
    }

}
