package com.example.uiedvideocompacter.ui.screens.library

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.uiedvideocompacter.data.model.CompressionPreset
import com.example.uiedvideocompacter.data.model.VideoItem
import com.example.uiedvideocompacter.data.repository.VideoRepository
import com.example.uiedvideocompacter.data.store.QueueItemData
import com.example.uiedvideocompacter.data.store.QueueStore
import com.example.uiedvideocompacter.data.store.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VideoRepository(application)
    private val userPreferences = UserPreferences(application)
    private val queueStore = QueueStore(application)

    val showResolution = userPreferences.showResolution
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val maxSelection = userPreferences.maxSelection
        .stateIn(viewModelScope, SharingStarted.Lazily, 100)
        
    val isGridView = userPreferences.isGridView
        .stateIn(viewModelScope, SharingStarted.Lazily, true)
        
    private val _sortOrder = MutableStateFlow(0)
    val sortOrder = _sortOrder.asStateFlow()

    // Search State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _searchActive = MutableStateFlow(false)
    val searchActive = _searchActive.asStateFlow()

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions = _suggestions.asStateFlow()

    private var allTagsIndex = setOf<String>()

    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos: StateFlow<List<VideoItem>> = kotlinx.coroutines.flow.combine(
        _videos, _sortOrder, _searchQuery
    ) { list, order, query ->
        val filtered = if (query.isEmpty()) {
            list
        } else {
            list.filter { it.name.contains(query, ignoreCase = true) }
        }
        
        when (order) {
            0 -> filtered.sortedByDescending { it.dateAdded }
            1 -> filtered.sortedBy { it.dateAdded }
            2 -> filtered.sortedByDescending { it.size }
            3 -> filtered.sortedBy { it.size }
            4 -> filtered.sortedBy { it.name }
            else -> filtered.sortedByDescending { it.dateAdded }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _selectedVideos = MutableStateFlow<Set<Long>>(emptySet())
    val selectedVideos: StateFlow<Set<Long>> = _selectedVideos.asStateFlow()

    init {
        // Debounce search query for suggestions (120ms)
        viewModelScope.launch {
            _searchQuery
                .debounce(120)
                .collect { query ->
                    if (query.isNotEmpty() && _searchActive.value) {
                        generateSuggestions(query)
                    } else {
                        _suggestions.value = emptyList()
                    }
                }
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onSearchActiveChange(active: Boolean) {
        _searchActive.value = active
        if (active) {
            buildTagIndex()
        }
    }

    private fun buildTagIndex() {
        viewModelScope.launch(Dispatchers.Default) {
            // Limit indexing to 600 videos for performance on older devices
            val indexList = _videos.value.take(600)
            allTagsIndex = indexList.flatMap { 
                com.example.uiedvideocompacter.data.model.SearchSuggestionTags.extractTags(it.name) 
            }.toSet()
        }
    }

    private fun generateSuggestions(query: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val result = com.example.uiedvideocompacter.data.model.SearchSuggestionTags.generateSuggestions(
                query, allTagsIndex, maxResults = 10
            )
            _suggestions.value = result
        }
    }

    var isLoading by mutableStateOf(false)
        private set

    private var currentOffset = 0
    private val pageSize = 50
    private var isLastPage = false
    
    fun toggleViewMode() {
        viewModelScope.launch {
            val current = isGridView.first()
            userPreferences.setGridView(!current)
        }
    }
    
    fun setSortOrder(order: Int) {
        viewModelScope.launch {
            _sortOrder.value = order
            userPreferences.setSortOrder(order)
        }
    }

    fun loadVideos(reset: Boolean = false) {
        if (isLoading) return
        if (reset) {
            currentOffset = 0
            isLastPage = false
            _videos.value = emptyList()
        }
        
        if (isLastPage) return

        viewModelScope.launch {
            isLoading = true
            val result = withContext(Dispatchers.IO) {
                repository.getVideos(limit = pageSize, offset = currentOffset)
            }
            
            result.onSuccess { newVideos ->
                if (newVideos.size < pageSize) {
                    isLastPage = true
                }
                
                currentOffset += newVideos.size
                if (reset) {
                    _videos.value = newVideos
                } else {
                    _videos.value += newVideos
                }
            }.onFailure { e ->
                // Handle error (e.g., show snackbar)
                // For now, we just log it or leave the list as is
                e.printStackTrace()
            }
            isLoading = false
        }
    }

    suspend fun toggleSelection(video: VideoItem) {
        val currentSelection: MutableSet<Long> = _selectedVideos.value.toMutableSet()
        val max = maxSelection.first()
        if (currentSelection.contains(video.id)) {
            currentSelection.remove(video.id)
        } else {
            if (currentSelection.size < max) {
                currentSelection.add(video.id)
            }
        }
        _selectedVideos.value = currentSelection
    }

    fun clearSelection() {
        _selectedVideos.value = emptySet<Long>()
    }

    fun selectAll() {
        viewModelScope.launch {
            val max = maxSelection.first()
            _selectedVideos.value = _videos.value.take(max).map { it.id }.toSet()
        }
    }

    suspend fun addToQueue() {
        val selectedIds = _selectedVideos.value
        val selectedVideos = _videos.value.filter { selectedIds.contains(it.id) }
        val queueItems = selectedVideos.map { video ->
            QueueItemData(
                id = java.util.UUID.randomUUID().toString(),
                name = video.name,
                uri = video.uri.toString(),
                size = video.size,
                duration = video.duration,
                presetName = CompressionPreset.BALANCED.name
            )
        }
        queueStore.addAllToQueue(queueItems)
        clearSelection()
    }
}