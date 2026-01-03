package com.example.uiedvideocompacter.ui.screens.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.uiedvideocompacter.data.store.UserPreferences
import kotlinx.coroutines.launch

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {
    private val userPreferences = UserPreferences(application)

    fun setOnboardingCompleted() {
        viewModelScope.launch {
            userPreferences.setOnboardingCompleted()
        }
    }
}
