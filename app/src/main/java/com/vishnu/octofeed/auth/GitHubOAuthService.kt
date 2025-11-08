package com.vishnu.octofeed.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class GitHubOAuthService(private val context: Context) {

    private val client = OkHttpClient()
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    /**
     * Start the OAuth flow by opening GitHub's authorization page in a browser
     */
    fun startOAuthFlow() {
        val authUrl = Uri.parse(GitHubOAuthConfig.AUTHORIZATION_URL).buildUpon()
            .appendQueryParameter("client_id", GitHubOAuthConfig.CLIENT_ID)
            .appendQueryParameter("redirect_uri", GitHubOAuthConfig.REDIRECT_URI)
            .appendQueryParameter("scope", "user:email read:user")
            .build()

        val intent = Intent(Intent.ACTION_VIEW, authUrl)
        context.startActivity(intent)
    }

    /**
     * Exchange the authorization code for an access token
     */
    suspend fun getAccessToken(code: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val requestBody = FormBody.Builder()
                .add("client_id", GitHubOAuthConfig.CLIENT_ID)
                .add("client_secret", GitHubOAuthConfig.CLIENT_SECRET)
                .add("code", code)
                .add("redirect_uri", GitHubOAuthConfig.REDIRECT_URI)
                .build()

            val request = Request.Builder()
                .url(GitHubOAuthConfig.ACCESS_TOKEN_URL)
                .post(requestBody)
                .addHeader("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Failed to get access token: ${response.code}"))
                }

                val responseBody = response.body?.string()
                    ?: return@withContext Result.failure(IOException("Empty response body"))

                val tokenResponse = json.decodeFromString<GitHubAccessTokenResponse>(responseBody)

                if (tokenResponse.error != null) {
                    return@withContext Result.failure(
                        IOException("GitHub OAuth error: ${tokenResponse.error} - ${tokenResponse.error_description}")
                    )
                }

                tokenResponse.access_token?.let { token ->
                    Result.success(token)
                } ?: Result.failure(IOException("No access token in response"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch the authenticated user's information from GitHub API
     */
    suspend fun getUserInfo(accessToken: String): Result<GitHubUser> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(GitHubOAuthConfig.USER_API_URL)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Failed to get user info: ${response.code}"))
                }

                val responseBody = response.body?.string()
                    ?: return@withContext Result.failure(IOException("Empty response body"))

                val user = json.decodeFromString<GitHubUser>(responseBody)
                Result.success(user)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

