package com.vishnu.octofeed.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class GitHubApiService {
    private val client = OkHttpClient()
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    suspend fun getReceivedEvents(
        username: String,
        accessToken: String
    ): Result<List<GitHubEvent>> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://api.github.com/users/$username/received_events")
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Accept", "application/vnd.github.v3+json")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IOException("Failed to fetch events: ${response.code}")
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

    suspend fun getRepositoryDetails(
        repoFullName: String,
        accessToken: String
    ): Result<RepositoryDetails> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://api.github.com/repos/$repoFullName")
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Accept", "application/vnd.github.v3+json")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IOException("Failed to fetch repository: ${response.code}")
                        )
                    }

                    val responseBody = response.body?.string()
                        ?: return@withContext Result.failure(IOException("Empty response body"))

                    val repo = json.decodeFromString<RepositoryDetails>(responseBody)
                    Result.success(repo)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun starRepository(
        repoFullName: String,
        accessToken: String
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            Log.d("GitHubAPI", "starRepository() called for: $repoFullName")

            try {
                val url = "https://api.github.com/user/starred/$repoFullName"
                Log.d("GitHubAPI", "Star URL: $url")

                val request = Request.Builder()
                    .url(url)
                    .put(ByteArray(0).toRequestBody(null))
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Accept", "application/vnd.github.v3+json")
                    .addHeader("Content-Length", "0")
                    .build()

                client.newCall(request).execute().use { response ->
                    Log.d("GitHubAPI", "Star response code: ${response.code}")
                    if (!response.isSuccessful && response.code != 204) {
                        val errorBody = response.body?.string()
                        Log.e(
                            "GitHubAPI",
                            "Failed to star repository: ${response.code}, body: $errorBody"
                        )
                        return@withContext Result.failure(
                            IOException("Failed to star repository: ${response.code} - $errorBody")
                        )
                    }
                    Log.d("GitHubAPI", "Successfully starred repository")
                    Result.success(Unit)
                }
            } catch (e: Exception) {
                Log.e("GitHubAPI", "Exception starring repository", e)
                Result.failure(e)
            }
        }

    suspend fun checkIfStarred(
        repoFullName: String,
        accessToken: String
    ): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val url = "https://api.github.com/user/starred/$repoFullName"
                Log.d("GitHubAPI", "checkIfStarred URL: $url")

                val request = Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Accept", "application/vnd.github.v3+json")
                    .build()

                client.newCall(request).execute().use { response ->
                    Log.d("GitHubAPI", "checkIfStarred response code: ${response.code}")
                    // 204 means starred, 404 means not starred
                    val isStarred = response.code == 204
                    Log.d("GitHubAPI", "Repository $repoFullName is starred: $isStarred")
                    Result.success(isStarred)
                }
            } catch (e: Exception) {
                Log.e("GitHubAPI", "Exception checking star status", e)
                Result.failure(e)
            }
        }

    suspend fun unstarRepository(
        repoFullName: String,
        accessToken: String
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            Log.d("GitHubAPI", "unstarRepository() called for: $repoFullName")

            try {
                val url = "https://api.github.com/user/starred/$repoFullName"
                Log.d("GitHubAPI", "Unstar URL: $url")

                val request = Request.Builder()
                    .url(url)
                    .delete()
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Accept", "application/vnd.github.v3+json")
                    .build()

                client.newCall(request).execute().use { response ->
                    Log.d("GitHubAPI", "Unstar response code: ${response.code}")
                    if (!response.isSuccessful && response.code != 204) {
                        val errorBody = response.body?.string()
                        Log.e(
                            "GitHubAPI",
                            "Failed to unstar repository: ${response.code}, body: $errorBody"
                        )
                        return@withContext Result.failure(
                            IOException("Failed to unstar repository: ${response.code} - $errorBody")
                        )
                    }
                    Log.d("GitHubAPI", "Successfully unstarred repository")
                    Result.success(Unit)
                }
            } catch (e: Exception) {
                Log.e("GitHubAPI", "Exception unstarring repository", e)
                Result.failure(e)
            }
        }

    suspend fun getFollowers(
        username: String,
        accessToken: String
    ): Result<List<UserFollower>> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://api.github.com/users/$username/followers?per_page=30")
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Accept", "application/vnd.github.v3+json")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IOException("Failed to fetch followers: ${response.code}")
                        )
                    }

                    val responseBody = response.body?.string()
                        ?: return@withContext Result.failure(IOException("Empty response body"))

                    val followers = json.decodeFromString<List<UserFollower>>(responseBody)
                    Result.success(followers)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getFollowing(
        username: String,
        accessToken: String
    ): Result<List<UserFollowing>> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://api.github.com/users/$username/following?per_page=30")
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Accept", "application/vnd.github.v3+json")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IOException("Failed to fetch following: ${response.code}")
                        )
                    }

                    val responseBody = response.body?.string()
                        ?: return@withContext Result.failure(IOException("Empty response body"))

                    val following = json.decodeFromString<List<UserFollowing>>(responseBody)
                    Result.success(following)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getUserFollowing(
        username: String,
        accessToken: String
    ): Result<List<UserFollowing>> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://api.github.com/users/$username/following?per_page=10")
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Accept", "application/vnd.github.v3+json")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IOException("Failed to fetch user following: ${response.code}")
                        )
                    }

                    val responseBody = response.body?.string()
                        ?: return@withContext Result.failure(IOException("Empty response body"))

                    val following = json.decodeFromString<List<UserFollowing>>(responseBody)
                    Result.success(following)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}

