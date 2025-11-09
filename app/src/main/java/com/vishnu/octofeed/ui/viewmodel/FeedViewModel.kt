package com.vishnu.octofeed.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vishnu.octofeed.data.GitHubApiService
import com.vishnu.octofeed.data.GitHubEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class FeedUiState {
    object Loading : FeedUiState()
    data class Success(val events: List<GitHubEvent>) : FeedUiState()
    data class Error(val message: String) : FeedUiState()
}

class FeedViewModel : ViewModel() {
    private val apiService = GitHubApiService()

    private val _uiState = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    fun loadFeed(username: String, accessToken: String) {
        viewModelScope.launch {
            _uiState.value = FeedUiState.Loading

            apiService.getReceivedEvents(username, accessToken).fold(
                onSuccess = { events ->
                    _uiState.value = FeedUiState.Success(events)
                },
                onFailure = { exception ->
                    _uiState.value = FeedUiState.Error(
                        exception.message ?: "Failed to load feed"
                    )
                }
            )
        }
    }

    fun refresh(username: String, accessToken: String) {
        loadFeed(username, accessToken)
    }
}

