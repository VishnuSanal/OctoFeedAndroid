package com.vishnu.octofeed.auth

import kotlinx.serialization.Serializable

@Serializable
data class GitHubAccessTokenResponse(
    val access_token: String? = null,
    val token_type: String? = null,
    val scope: String? = null,
    val error: String? = null,
    val error_description: String? = null
)

@Serializable
data class GitHubUser(
    val login: String,
    val id: Long,
    val avatar_url: String,
    val name: String?,
    val email: String?,
    val bio: String?,
    val public_repos: Int,
    val followers: Int,
    val following: Int
)

