package com.vishnu.octofeed.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vishnu.octofeed.data.GitHubApiService
import com.vishnu.octofeed.data.GitHubEvent
import com.vishnu.octofeed.data.RepositoryDetails
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

    // Cache for repository details
    private val _repoDetailsCache = MutableStateFlow<Map<String, RepositoryDetails>>(emptyMap())
    val repoDetailsCache: StateFlow<Map<String, RepositoryDetails>> =
        _repoDetailsCache.asStateFlow()

    // Cache for starred status
    private val _starredCache = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val starredCache: StateFlow<Map<String, Boolean>> = _starredCache.asStateFlow()

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

    fun loadRepositoryDetails(repoFullName: String, accessToken: String) {
        viewModelScope.launch {
            // Check if already cached
            if (_repoDetailsCache.value.containsKey(repoFullName)) {
                return@launch
            }

            apiService.getRepositoryDetails(repoFullName, accessToken).fold(
                onSuccess = { details ->
                    _repoDetailsCache.value = _repoDetailsCache.value + (repoFullName to details)
                },
                onFailure = {
                    it.printStackTrace()
                }
            )
        }
    }

    fun checkIfStarred(repoFullName: String, accessToken: String) {
        viewModelScope.launch {
            apiService.checkIfStarred(repoFullName, accessToken).fold(
                onSuccess = { isStarred ->
                    _starredCache.value = _starredCache.value + (repoFullName to isStarred)
                },
                onFailure = { error ->
                    Log.e("FeedViewModel", "Failed to check star status for $repoFullName", error)
                }
            )
        }
    }

    fun toggleStar(repoFullName: String, accessToken: String) {
        viewModelScope.launch {
            val isCurrentlyStarred = _starredCache.value[repoFullName] ?: false

            Log.d(
                "FeedViewModel",
                "Toggling star for $repoFullName, currently starred: $isCurrentlyStarred"
            )

            val result = if (isCurrentlyStarred) {
                apiService.unstarRepository(repoFullName, accessToken)
            } else {
                apiService.starRepository(repoFullName, accessToken)
            }

            result.fold(
                onSuccess = {
                    Log.d("FeedViewModel", "Successfully toggled star for $repoFullName")
                    _starredCache.value =
                        _starredCache.value + (repoFullName to !isCurrentlyStarred)
                    // Update the star count in the cache
                    _repoDetailsCache.value[repoFullName]?.let { details ->
                        val updatedDetails = details.copy(
                            stargazersCount = if (isCurrentlyStarred)
                                details.stargazersCount - 1
                            else
                                details.stargazersCount + 1
                        )
                        _repoDetailsCache.value =
                            _repoDetailsCache.value + (repoFullName to updatedDetails)
                    }
                },
                onFailure = { error ->
                    Log.e("FeedViewModel", "Failed to toggle star for $repoFullName", error)
                }
            )
        }
    }
}
