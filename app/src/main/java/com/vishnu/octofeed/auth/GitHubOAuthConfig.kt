package com.vishnu.octofeed.auth

import com.vishnu.octofeed.BuildConfig

object GitHubOAuthConfig {
    // Register your app at: https://github.com/settings/developers
    const val CLIENT_ID = BuildConfig.CLIENT_ID
    const val CLIENT_SECRET = BuildConfig.CLIENT_SECRET

    const val REDIRECT_URI = "octofeed://github-auth"
    const val AUTHORIZATION_URL = "https://github.com/login/oauth/authorize"
    const val ACCESS_TOKEN_URL = "https://github.com/login/oauth/access_token"
    const val USER_API_URL = "https://api.github.com/user"
}

