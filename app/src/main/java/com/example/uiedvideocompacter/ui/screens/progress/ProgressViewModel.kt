package com.example.uiedvideocompacter.ui.screens.progress

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.work.WorkInfo
import androidx.work.WorkManager

class ProgressViewModel(application: Application) : AndroidViewModel(application) {
    private val workManager = WorkManager.getInstance(application)

    // Observe all works with tag "compression"
    val compressionWorkInfos: LiveData<List<WorkInfo>> = workManager
        .getWorkInfosByTagLiveData("compression")
        
    fun cancelWork(id: java.util.UUID) {
        workManager.cancelWorkById(id)
    }
}
