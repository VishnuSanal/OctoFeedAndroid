package com.vishnu.octofeed.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vishnu.octofeed.data.FollowerEvent
import com.vishnu.octofeed.data.FollowerEventType
import com.vishnu.octofeed.data.GitHubApiService
import com.vishnu.octofeed.data.GitHubEvent
import com.vishnu.octofeed.data.RepositoryDetails
import com.vishnu.octofeed.data.UserFollower
import com.vishnu.octofeed.data.UserFollowing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.collections.take

sealed class FeedUiState {
    object Loading : FeedUiState()
    data class Success(
        val events: List<GitHubEvent>,
        val followerEvents: List<FollowerEvent>
    ) : FeedUiState()
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

    // Store previous followers to detect new ones
    private var previousFollowers: Set<String> = emptySet()
    private var previousFollowingByUser: Map<String, Set<String>> = emptyMap()

    fun loadFeed(username: String, accessToken: String) {
        viewModelScope.launch {
            _uiState.value = FeedUiState.Loading

            // Fetch regular events
            val eventsResult = apiService.getReceivedEvents(username, accessToken)

            // Fetch follower data
            val followersResult = apiService.getFollowers(username, accessToken)
            val followingResult = apiService.getFollowing(username, accessToken)

            if (eventsResult.isSuccess) {
                val events = eventsResult.getOrNull() ?: emptyList()
                val followers = followersResult.getOrNull() ?: emptyList()
                val following = followingResult.getOrNull() ?: emptyList()

                // Generate follower events
                val followerEvents = generateFollowerEvents(
                    username = username,
                    currentFollowers = followers,
                    following = following,
                    accessToken = accessToken
                )

                _uiState.value = FeedUiState.Success(
                    events = events,
                    followerEvents = followerEvents
                )
            } else {
                _uiState.value = FeedUiState.Error(
                    eventsResult.exceptionOrNull()?.message ?: "Failed to load feed"
                )
            }
        }
    }

    private suspend fun generateFollowerEvents(
        username: String,
        currentFollowers: List<UserFollower>,
        following: List<UserFollowing>,
        accessToken: String
    ): List<FollowerEvent> {
        val followerEvents = mutableListOf<FollowerEvent>()
        val now = Instant.now().toString()

        // Detect new followers
        val currentFollowerLogins = currentFollowers.map { it.login }.toSet()
        val newFollowers = if (previousFollowers.isEmpty()) {
            // First load - show recent followers but don't mark all as "new"
            currentFollowers.take(3)
        } else {
            currentFollowers.filter { it.login !in previousFollowers }
        }

        newFollowers.forEach { follower ->
            followerEvents.add(
                FollowerEvent(
                    type = FollowerEventType.NEW_FOLLOWER,
                    user = follower,
                    targetUser = null,
                    timestamp = now
                )
            )
        }
        previousFollowers = currentFollowerLogins

        // Check following activity for users you follow
        // For each user you follow, check who they're following
        following.take(5).forEach { followedUser ->
            try {
                val theirFollowing = apiService.getUserFollowing(followedUser.login, accessToken)
                    .getOrNull() ?: emptyList()

                val currentlyFollowing = theirFollowing.map { it.login }.toSet()
                val previouslyFollowing = previousFollowingByUser[followedUser.login] ?: emptySet()

                // Only add events for new following (not on first load)
                if (previouslyFollowing.isNotEmpty()) {
                    val newFollowing = theirFollowing.filter { it.login !in previouslyFollowing }

                    newFollowing.take(2).forEach { targetUser ->
                        followerEvents.add(
                            FollowerEvent(
                                type = FollowerEventType.FOLLOWING_ACTIVITY,
                                user = UserFollower(
                                    login = followedUser.login,
                                    id = followedUser.id,
                                    avatarUrl = followedUser.avatarUrl,
                                    htmlUrl = followedUser.htmlUrl,
                                    type = followedUser.type,
                                    siteAdmin = followedUser.siteAdmin
                                ),
                                targetUser = UserFollower(
                                    login = targetUser.login,
                                    id = targetUser.id,
                                    avatarUrl = targetUser.avatarUrl,
                                    htmlUrl = targetUser.htmlUrl,
                                    type = targetUser.type,
                                    siteAdmin = targetUser.siteAdmin
                                ),
                                timestamp = now
                            )
                        )
                    }
                }

                previousFollowingByUser = previousFollowingByUser + (followedUser.login to currentlyFollowing)
            } catch (e: Exception) {
                Log.e("FeedViewModel", "Error checking following for ${followedUser.login}", e)
            }
        }

        return followerEvents
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
