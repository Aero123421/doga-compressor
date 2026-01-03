package com.example.uiedvideocompacter.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.uiedvideocompacter.data.store.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val userPreferences = UserPreferences(application)

    val showResolution: StateFlow<Boolean> = userPreferences.showResolution
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val maxSelection: StateFlow<Int> = userPreferences.maxSelection
        .stateIn(viewModelScope, SharingStarted.Lazily, 100)

    val maxParallelTasks: StateFlow<Int> = userPreferences.maxParallelTasks
        .stateIn(viewModelScope, SharingStarted.Lazily, 1)

    fun toggleShowResolution(show: Boolean) {
        viewModelScope.launch {
            userPreferences.setShowResolution(show)
        }
    }

    fun updateMaxSelection(max: Int) {
        viewModelScope.launch {
            userPreferences.setMaxSelection(max)
        }
    }

    fun setMaxParallelTasks(max: Int) {
        viewModelScope.launch {
            userPreferences.setMaxParallelTasks(max)
        }
    }
}
