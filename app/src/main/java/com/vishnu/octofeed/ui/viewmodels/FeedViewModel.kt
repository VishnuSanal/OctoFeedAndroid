package com.vishnu.octofeed.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vishnu.octofeed.data.api.GitHubApiService
import com.vishnu.octofeed.data.cache.FollowersCache
import com.vishnu.octofeed.data.models.FeedEvent
import com.vishnu.octofeed.data.repository.EventsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed class FeedUiState {
    object Loading : FeedUiState()
    data class Success(val events: List<FeedEvent>) : FeedUiState()
    data class Error(val message: String) : FeedUiState()
}

class FeedViewModel(
    private val accessToken: String,
    private val username: String,
    private val cacheDir: File
) : ViewModel() {

    private val apiService = GitHubApiService(accessToken, cacheDir)
    private val followersCache = FollowersCache(cacheDir)
    private val repository = EventsRepository(apiService, username, followersCache)

    private val _uiState = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    init {
        loadEvents()
    }

    fun loadEvents() {
        viewModelScope.launch {
            _uiState.value = FeedUiState.Loading

            val result = repository.getEvents(username)

            _uiState.value = if (result.isSuccess) {
                val events = result.getOrNull() ?: emptyList()
                if (events.isEmpty()) {
                    FeedUiState.Error("No recent activity from people you follow")
                } else {
                    FeedUiState.Success(events)
                }
            } else {
                FeedUiState.Error(result.exceptionOrNull()?.message ?: "Failed to load events")
            }
        }
    }

    fun refreshEvents() {
        loadEvents()
    }

    fun toggleStar(
        repoFullName: String,
        isCurrentlyStarred: Boolean,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val result = if (isCurrentlyStarred) {
                repository.unstarRepository(repoFullName)
            } else {
                repository.starRepository(repoFullName)
            }

            if (result.isSuccess) {
                onComplete(!isCurrentlyStarred)
            } else {
                Log.e(
                    "FeedViewModel",
                    "Failed to toggle star: ${result.exceptionOrNull()?.message}"
                )
                onComplete(isCurrentlyStarred) // Keep current state on failure
            }
        }
    }

    fun checkStarStatus(repoFullName: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.isRepositoryStarred(repoFullName)
            onResult(result.getOrDefault(false))
        }
    }
}

