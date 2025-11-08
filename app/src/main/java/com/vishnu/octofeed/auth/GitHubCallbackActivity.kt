package com.vishnu.octofeed.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.vishnu.octofeed.MainActivity
import kotlinx.coroutines.launch

class GitHubCallbackActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get the authorization code from the callback URI
        val uri = intent?.data
        val code = uri?.getQueryParameter("code")
        val error = uri?.getQueryParameter("error")

        if (error != null) {
            Toast.makeText(this, "Authorization failed: $error", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        if (code != null) {
            handleAuthorizationCode(code)
        } else {
            Toast.makeText(this, "No authorization code received", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun handleAuthorizationCode(code: String) {
        val oauthService = GitHubOAuthService(this)
        val tokenManager = TokenManager(this)

        lifecycleScope.launch {
            try {
                // Exchange code for access token
                val tokenResult = oauthService.getAccessToken(code)

                tokenResult.fold(
                    onSuccess = { accessToken ->
                        // Save the access token
                        tokenManager.saveAccessToken(accessToken)

                        // Fetch user info
                        val userResult = oauthService.getUserInfo(accessToken)
                        userResult.fold(
                            onSuccess = { user ->
                                tokenManager.saveUserInfo(user)
                                Toast.makeText(
                                    this@GitHubCallbackActivity,
                                    "Welcome, ${user.name ?: user.login}!",
                                    Toast.LENGTH_SHORT
                                ).show()

                                // Navigate to main activity
                                val intent =
                                    Intent(this@GitHubCallbackActivity, MainActivity::class.java)
                                intent.flags =
                                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                            },
                            onFailure = { exception ->
                                Toast.makeText(
                                    this@GitHubCallbackActivity,
                                    "Failed to get user info: ${exception.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                    },
                    onFailure = { exception ->
                        Toast.makeText(
                            this@GitHubCallbackActivity,
                            "Authentication failed: ${exception.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(
                    this@GitHubCallbackActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                finish()
            }
        }
    }
}

