package com.vishnu.octofeed.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
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
}

