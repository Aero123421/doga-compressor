package com.example.uiedvideocompacter.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferences(private val context: Context) {

    companion object {
        val KEY_SHOW_RESOLUTION = booleanPreferencesKey("show_resolution")
        val KEY_MAX_SELECTION = intPreferencesKey("max_selection")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_IS_GRID_VIEW = booleanPreferencesKey("is_grid_view")
        val KEY_SORT_ORDER = intPreferencesKey("sort_order")
        val KEY_USE_HEVC = booleanPreferencesKey("use_hevc")
        val KEY_MAX_PARALLEL_TASKS = intPreferencesKey("max_parallel_tasks")
    }

    val showResolution: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_SHOW_RESOLUTION] ?: false
        }

    val maxSelection: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_MAX_SELECTION] ?: 100
        }

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] ?: false
        }

    val isGridView: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_IS_GRID_VIEW] ?: true
        }

    val sortOrder: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_SORT_ORDER] ?: 0 // 0: Date Desc
        }

    val useHevc: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_USE_HEVC] ?: false // Default to false for compatibility
        }

    val maxParallelTasks: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_MAX_PARALLEL_TASKS] ?: 1
        }

    suspend fun setShowResolution(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SHOW_RESOLUTION] = show
        }
    }

    suspend fun setMaxSelection(max: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_MAX_SELECTION] = max
        }
    }

    suspend fun setOnboardingCompleted() {
        context.dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] = true
        }
    }

    suspend fun setGridView(isGrid: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_GRID_VIEW] = isGrid
        }
    }

    suspend fun setSortOrder(order: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SORT_ORDER] = order
        }
    }

    suspend fun setUseHevc(useHevc: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_USE_HEVC] = useHevc
        }
    }

    suspend fun setMaxParallelTasks(max: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_MAX_PARALLEL_TASKS] = max.coerceIn(1, 3)
        }
    }
}
