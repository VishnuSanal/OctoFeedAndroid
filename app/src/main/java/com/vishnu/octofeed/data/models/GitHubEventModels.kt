package com.vishnu.octofeed.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * GitHub Event from API
 */
@Serializable
data class GitHubEvent(
    val id: String,
    val type: String,
    val actor: Actor,
    val repo: Repo,
    val payload: Payload? = null,
    @SerialName("created_at")
    val createdAt: String,
    val public: Boolean = true
)

@Serializable
data class Actor(
    val id: Long,
    val login: String,
    @SerialName("display_login")
    val displayLogin: String? = null,
    @SerialName("gravatar_id")
    val gravatarId: String? = null,
    val url: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String
)

@Serializable
data class Repo(
    val id: Long,
    val name: String,
    val url: String
)

@Serializable
data class Payload(
    val action: String? = null,
    val ref: String? = null,
    @SerialName("ref_type")
    val refType: String? = null,
    val forkee: Forkee? = null,
    val release: Release? = null,
    val repository: Repository? = null
)

@Serializable
data class Forkee(
    val id: Long,
    @SerialName("full_name")
    val fullName: String,
    val owner: Owner,
    @SerialName("html_url")
    val htmlUrl: String
)

@Serializable
data class Owner(
    val login: String,
    @SerialName("avatar_url")
    val avatarUrl: String
)

@Serializable
data class Release(
    val id: Long,
    @SerialName("tag_name")
    val tagName: String,
    val name: String? = null,
    @SerialName("html_url")
    val htmlUrl: String
)

@Serializable
data class Repository(
    val id: Long,
    val name: String,
    @SerialName("full_name")
    val fullName: String
)

/**
 * Processed events for UI display
 */
sealed class FeedEvent {
    abstract val id: String
    abstract val actor: Actor
    abstract val timestamp: String

    data class FollowEvent(
        override val id: String,
        override val actor: Actor,
        override val timestamp: String
    ) : FeedEvent()

    data class StarEvent(
        override val id: String,
        override val actor: Actor,
        override val timestamp: String,
        val repo: Repo
    ) : FeedEvent()

    data class ForkEvent(
        override val id: String,
        override val actor: Actor,
        override val timestamp: String,
        val repo: Repo,
        val forkedRepo: String?
    ) : FeedEvent()

    data class CreateRepoEvent(
        override val id: String,
        override val actor: Actor,
        override val timestamp: String,
        val repo: Repo
    ) : FeedEvent()

    data class ReleaseEvent(
        override val id: String,
        override val actor: Actor,
        override val timestamp: String,
        val repo: Repo,
        val releaseName: String,
        val tagName: String
    ) : FeedEvent()
}

