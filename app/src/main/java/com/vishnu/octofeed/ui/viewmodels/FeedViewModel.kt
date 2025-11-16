package com.vishnu.octofeed.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vishnu.octofeed.data.api.GitHubApiService
import com.vishnu.octofeed.data.models.FeedEvent
import com.vishnu.octofeed.data.repository.EventsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class FeedUiState {
    object Loading : FeedUiState()
    data class Success(val events: List<FeedEvent>) : FeedUiState()
    data class Error(val message: String) : FeedUiState()
}

class FeedViewModel(
    private val accessToken: String,
    private val username: String
) : ViewModel() {

    private val apiService = GitHubApiService(accessToken)
    private val repository = EventsRepository(apiService, username)

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
                    Log.e("vishnu", "loadEvents: $events")
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
}

