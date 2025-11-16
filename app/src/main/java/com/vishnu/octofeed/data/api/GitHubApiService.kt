package com.vishnu.octofeed.data.api

import com.vishnu.octofeed.data.models.GitHubEvent
import com.vishnu.octofeed.data.models.RepoDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

class GitHubApiService(
    private val accessToken: String,
    private val cacheDir: File
) {

    private val client = OkHttpClient.Builder()
        .cache(
            Cache(
                directory = File(cacheDir, "http_api_cache"),
                maxSize = 100L * 1024L * 1024L // 50 MiB
            )
        )
        .build()
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    companion object {
        private const val BASE_URL = "https://api.github.com"
    }

    /**
     * Get list of users the authenticated user is following
     */
    suspend fun getUserFollowing(
        username: String,
        page: Int = 1,
        perPage: Int = 100
    ): Result<List<String>> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$BASE_URL/users/$username/following?page=$page&per_page=$perPage")
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Accept", "application/vnd.github+json")
                    .addHeader("X-GitHub-Api-Version", "2022-11-28")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IOException("Failed to get following list: ${response.code}")
                        )
                    }

                    val responseBody = response.body?.string()
                        ?: return@withContext Result.failure(IOException("Empty response body"))

                    val followingArray = json.parseToJsonElement(responseBody)
                    val followingList = mutableListOf<String>()

                    if (followingArray is kotlinx.serialization.json.JsonArray) {
                        followingArray.forEach { element ->
                            val obj = element as? kotlinx.serialization.json.JsonObject
                            val login = obj?.get("login")?.let {
                                (it as? kotlinx.serialization.json.JsonPrimitive)?.content
                            }
                            login?.let { followingList.add(it) }
                        }
                    }

                    Result.success(followingList)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Get list of users following the authenticated user
     */
    @kotlinx.serialization.Serializable
    data class FollowerInfo(
        val login: String,
        @kotlinx.serialization.SerialName("avatar_url")
        val avatarUrl: String,
        val id: Long
    )

    suspend fun getUserFollowers(
        username: String,
        page: Int = 1,
        perPage: Int = 100
    ): Result<List<FollowerInfo>> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$BASE_URL/users/$username/followers?page=$page&per_page=$perPage")
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Accept", "application/vnd.github+json")
                    .addHeader("X-GitHub-Api-Version", "2022-11-28")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IOException("Failed to get followers list: ${response.code}")
                        )
                    }

                    val responseBody = response.body?.string()
                        ?: return@withContext Result.failure(IOException("Empty response body"))

                    val followers = json.decodeFromString<List<FollowerInfo>>(responseBody)
                    Result.success(followers)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Get public events performed by a user
     */
    suspend fun getUserEvents(
        username: String,
        page: Int = 1,
        perPage: Int = 30
    ): Result<List<GitHubEvent>> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$BASE_URL/users/$username/events?page=$page&per_page=$perPage")
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Accept", "application/vnd.github+json")
                    .addHeader("X-GitHub-Api-Version", "2022-11-28")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IOException("Failed to get user events: ${response.code} - ${response.message}")
                        )
                    }

                    val responseBody = response.body?.string()
                        ?: return@withContext Result.failure(IOException("Empty response body"))

                    val events = json.decodeFromString<List<GitHubEvent>>(responseBody)
                    Result.success(events)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Get detailed repository information
     * @param owner Repository owner username
     * @param repo Repository name
     */
    suspend fun getRepositoryDetails(owner: String, repo: String): Result<RepoDetails> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$BASE_URL/repos/$owner/$repo")
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Accept", "application/vnd.github+json")
                    .addHeader("X-GitHub-Api-Version", "2022-11-28")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IOException("Failed to get repository details: ${response.code} - ${response.message}")
                        )
                    }

                    val responseBody = response.body?.string()
                        ?: return@withContext Result.failure(IOException("Empty response body"))

                    val repoDetails = json.decodeFromString<RepoDetails>(responseBody)
                    Result.success(repoDetails)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Check if a repository is starred by the authenticated user
     */
    suspend fun isRepositoryStarred(owner: String, repo: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$BASE_URL/user/starred/$owner/$repo")
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Accept", "application/vnd.github+json")
                    .addHeader("X-GitHub-Api-Version", "2022-11-28")
                    .build()

                client.newCall(request).execute().use { response ->
                    // 204 = starred, 404 = not starred
                    Result.success(response.code == 204)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Star a repository
     */
    suspend fun starRepository(owner: String, repo: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$BASE_URL/user/starred/$owner/$repo")
                    .put(okhttp3.RequestBody.create(null, ""))
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Accept", "application/vnd.github+json")
                    .addHeader("X-GitHub-Api-Version", "2022-11-28")
                    .build()

                client.newCall(request).execute().use { response ->
                    Result.success(response.isSuccessful)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Unstar a repository
     */
    suspend fun unstarRepository(owner: String, repo: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$BASE_URL/user/starred/$owner/$repo")
                    .delete()
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Accept", "application/vnd.github+json")
                    .addHeader("X-GitHub-Api-Version", "2022-11-28")
                    .build()

                client.newCall(request).execute().use { response ->
                    Result.success(response.isSuccessful)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}

