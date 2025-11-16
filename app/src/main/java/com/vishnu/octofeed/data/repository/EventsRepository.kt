package com.vishnu.octofeed.data.repository

import android.util.Log
import com.vishnu.octofeed.data.api.GitHubApiService
import com.vishnu.octofeed.data.cache.FollowersCache
import com.vishnu.octofeed.data.models.Actor
import com.vishnu.octofeed.data.models.FeedEvent
import com.vishnu.octofeed.data.models.GitHubEvent
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class EventsRepository(
    private val apiService: GitHubApiService,
    private val currentUsername: String,
    private val followersCache: FollowersCache
) {

    /**
     * Fetch and process all relevant events for the feed
     */
    suspend fun getEvents(username: String): Result<List<FeedEvent>> = coroutineScope {
        try {
            // Fetch list of users you're following
            val followingDeferred = async { apiService.getUserFollowing(currentUsername) }

            // Fetch current followers to detect new follows
            val followersDeferred = async { apiService.getUserFollowers(currentUsername) }

            // Wait for initial results
            val followingResult = followingDeferred.await()
            val followersResult = followersDeferred.await()

            // Get list of users being followed
            val following = followingResult.getOrNull() ?: emptyList()

            // add own username to fetch their events too
            val users = following + username

            // Fetch events from each user you follow (in parallel, limited to prevent rate limiting)
            val followingEvents = mutableListOf<GitHubEvent>()

            users.chunked(5).forEach { chunk ->
                val eventResults = chunk.map { username ->
                    async { apiService.getUserEvents(username, perPage = 10) }
                }
                eventResults.forEach { deferred ->
                    deferred.await().getOrNull()?.let { events ->
                        followingEvents.addAll(events)
                    }
                }
            }

            // Filter and convert to FeedEvents
            val feedEvents = processEvents(followingEvents).toMutableList()

            // Process followers and detect new follows
            val followers = followersResult.getOrNull()
            if (followers != null) {
                val cachedFollowers = followers.map {
                    FollowersCache.CachedFollower(
                        login = it.login,
                        avatarUrl = it.avatarUrl,
                        id = it.id
                    )
                }
                val newFollowers = followersCache.detectNewFollowers(cachedFollowers)

                // Create FollowEvents for new followers
                newFollowers.forEach { follower ->
                    feedEvents.add(
                        FeedEvent.FollowEvent(
                            id = "follow_${follower.id}_${follower.timestamp}",
                            actor = Actor(
                                id = follower.id,
                                login = follower.login,
                                displayLogin = follower.login,
                                gravatarId = null,
                                url = "https://github.com/${follower.login}",
                                avatarUrl = follower.avatarUrl
                            ),
                            timestamp = formatTimestamp(follower.timestamp)
                        )
                    )
                }
            }

            // Fetch repository details for events with repos
            val enrichedEvents = enrichEventsWithRepoDetails(feedEvents)

            Result.success(enrichedEvents)
        } catch (e: Exception) {
            Log.e("EventsRepository", "Error fetching events", e)
            Result.failure(e)
        }
    }

    /**
     * Format timestamp to ISO 8601 format
     */
    private fun formatTimestamp(timestamp: Long): String {
        val date = java.util.Date(timestamp)
        val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
        format.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return format.format(date)
    }

    /**
     * Process raw GitHub events into FeedEvents
     */
    private fun processEvents(events: List<GitHubEvent>): List<FeedEvent> {
        val feedEvents = mutableListOf<FeedEvent>()

        for (event in events) {
            when (event.type) {
                // Someone you follow starred a repo
                "WatchEvent" -> {
                    if (event.payload?.action == "started") {
                        feedEvents.add(
                            FeedEvent.StarEvent(
                                id = event.id,
                                actor = event.actor,
                                timestamp = event.createdAt,
                                repo = event.repo
                            )
                        )
                    }
                }

                // Someone you follow forked a repo
                "ForkEvent" -> {
                    feedEvents.add(
                        FeedEvent.ForkEvent(
                            id = event.id,
                            actor = event.actor,
                            timestamp = event.createdAt,
                            repo = event.repo,
                            forkedRepo = event.payload?.forkee?.fullName
                        )
                    )
                }

                // Someone you follow created a repo or branch
                "CreateEvent" -> {
                    // Only show repository creation, not branch creation
                    if (event.payload?.refType == "repository") {
                        feedEvents.add(
                            FeedEvent.CreateRepoEvent(
                                id = event.id,
                                actor = event.actor,
                                timestamp = event.createdAt,
                                repo = event.repo
                            )
                        )
                    }
                }

                // Someone you follow created a release
                "ReleaseEvent" -> {
                    if (event.payload?.action == "published") {
                        val release = event.payload.release
                        if (release != null) {
                            feedEvents.add(
                                FeedEvent.ReleaseEvent(
                                    id = event.id,
                                    actor = event.actor,
                                    timestamp = event.createdAt,
                                    repo = event.repo,
                                    releaseName = release.name ?: release.tagName,
                                    tagName = release.tagName
                                )
                            )
                        }
                    }
                }

                // Note: GitHub API doesn't provide FollowEvent in the events endpoint
                // We'll need to use a different approach or API for that
            }
        }

        return feedEvents.sortedByDescending { it.timestamp }
    }

    /**
     * Enrich events with repository details
     */
    private suspend fun enrichEventsWithRepoDetails(events: List<FeedEvent>): List<FeedEvent> =
        coroutineScope {
            events.map { event ->
                async {
                    when (event) {
                        is FeedEvent.StarEvent -> {
                            val repoDetails = fetchRepoDetails(event.repo.name)
                            event.copy(repoDetails = repoDetails)
                        }

                        is FeedEvent.ForkEvent -> {
                            val repoDetails = fetchRepoDetails(event.repo.name)
                            event.copy(repoDetails = repoDetails)
                        }

                        is FeedEvent.CreateRepoEvent -> {
                            val repoDetails = fetchRepoDetails(event.repo.name)
                            event.copy(repoDetails = repoDetails)
                        }

                        is FeedEvent.ReleaseEvent -> {
                            val repoDetails = fetchRepoDetails(event.repo.name)
                            event.copy(repoDetails = repoDetails)
                        }

                        else -> event
                    }
                }
            }.awaitAll()
        }

    /**
     * Fetch repository details from API
     */
    private suspend fun fetchRepoDetails(repoFullName: String): com.vishnu.octofeed.data.models.RepoDetails? {
        return try {
            val parts = repoFullName.split("/")
            if (parts.size == 2) {
                val (owner, repo) = parts
                apiService.getRepositoryDetails(owner, repo).getOrNull()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("EventsRepository", "Error fetching repo details for $repoFullName", e)
            null
        }
    }

    /**
     * Check if a repository is starred
     */
    suspend fun isRepositoryStarred(repoFullName: String): Result<Boolean> {
        return try {
            val parts = repoFullName.split("/")
            if (parts.size == 2) {
                val (owner, repo) = parts
                apiService.isRepositoryStarred(owner, repo)
            } else {
                Result.success(false)
            }
        } catch (e: Exception) {
            Log.e("EventsRepository", "Error checking star status for $repoFullName", e)
            Result.failure(e)
        }
    }

    /**
     * Star a repository
     */
    suspend fun starRepository(repoFullName: String): Result<Boolean> {
        return try {
            val parts = repoFullName.split("/")
            if (parts.size == 2) {
                val (owner, repo) = parts
                apiService.starRepository(owner, repo)
            } else {
                Result.failure(IllegalArgumentException("Invalid repository name"))
            }
        } catch (e: Exception) {
            Log.e("EventsRepository", "Error starring repository $repoFullName", e)
            Result.failure(e)
        }
    }

    /**
     * Unstar a repository
     */
    suspend fun unstarRepository(repoFullName: String): Result<Boolean> {
        return try {
            val parts = repoFullName.split("/")
            if (parts.size == 2) {
                val (owner, repo) = parts
                apiService.unstarRepository(owner, repo)
            } else {
                Result.failure(IllegalArgumentException("Invalid repository name"))
            }
        } catch (e: Exception) {
            Log.e("EventsRepository", "Error unstarring repository $repoFullName", e)
            Result.failure(e)
        }
    }
}

