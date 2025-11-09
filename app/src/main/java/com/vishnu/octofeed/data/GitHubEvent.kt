package com.vishnu.octofeed.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Generated data classes for the provided GitHub Events payload.
 *
 * These classes are compatible with kotlinx.serialization.
 *
 * Notes:
 * - Most fields are nullable because the payload is heterogeneous (different event types include different fields).
 * - Field names use camelCase in Kotlin and are mapped to the original JSON names with @SerialName where necessary.
 */

@Serializable
data class GitHubEvent(
    val id: String,
    val type: String,
    val actor: Actor,
    val repo: Repo,
    val payload: Payload? = null,
    @SerialName("public")
    val isPublic: Boolean,
    @SerialName("created_at")
    val createdAt: String,
    val org: Org? = null
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
    val avatarUrl: String? = null,
    @SerialName("html_url")
    val htmlUrl: String? = null,
    @SerialName("followers_url")
    val followersUrl: String? = null,
    @SerialName("following_url")
    val followingUrl: String? = null,
    @SerialName("gists_url")
    val gistsUrl: String? = null,
    @SerialName("starred_url")
    val starredUrl: String? = null,
    @SerialName("subscriptions_url")
    val subscriptionsUrl: String? = null,
    @SerialName("organizations_url")
    val organizationsUrl: String? = null,
    @SerialName("repos_url")
    val reposUrl: String? = null,
    @SerialName("events_url")
    val eventsUrl: String? = null,
    @SerialName("received_events_url")
    val receivedEventsUrl: String? = null,
    val type: String? = null,
    @SerialName("user_view_type")
    val userViewType: String? = null,
    @SerialName("site_admin")
    val siteAdmin: Boolean? = null
)

@Serializable
data class Repo(
    val id: Long,
    val name: String,
    val url: String
)

@Serializable
data class Org(
    val id: Long,
    val login: String,
    @SerialName("gravatar_id")
    val gravatarId: String? = null,
    val url: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null
)

@Serializable
data class Payload(
    // Branch / tag events
    val ref: String? = null,
    @SerialName("ref_type")
    val refType: String? = null,
    @SerialName("full_ref")
    val fullRef: String? = null,
    @SerialName("pusher_type")
    val pusherType: String? = null,
    @SerialName("master_branch")
    val masterBranch: String? = null,
    val description: String? = null,

    // Push events
    @SerialName("repository_id")
    val repositoryId: Long? = null,
    @SerialName("push_id")
    val pushId: Long? = null,
    val size: Int? = null,
    @SerialName("distinct_size")
    val distinctSize: Int? = null,
    @SerialName("head")
    val head: String? = null,
    @SerialName("before")
    val before: String? = null,

    // Generic action / number
    val action: String? = null,
    val number: Int? = null,

    // Pull request / review related
    val review: Review? = null,
    @SerialName("pull_request")
    val pullRequest: PullRequest? = null,

    // Issue / comment events
    val issue: Issue? = null,
    val comment: Comment? = null,

    // WatchEvent
    val repository: JsonElement? = null, // unused in sample but kept generic

    // Create/Delete events (already covered above by ref/ref_type), but keep generic fields
    @SerialName("full_name")
    val fullName: String? = null,

    // Labels and other collections
    val labels: List<Label>? = null,

    // Links (raw, often present for comments)
    @SerialName("_links")
    val links: Links? = null,

    // fallback for any unknown fields (useful if you decode with JsonElement in nested structures)
    val extra: JsonElement? = null
)

@Serializable
data class Review(
    val id: Long,
    @SerialName("node_id")
    val nodeId: String? = null,
    val user: UserSummary? = null,
    val body: String? = null,
    @SerialName("commit_id")
    val commitId: String? = null,
    @SerialName("submitted_at")
    val submittedAt: String? = null,
    val state: String? = null,
    @SerialName("html_url")
    val htmlUrl: String? = null,
    @SerialName("pull_request_url")
    val pullRequestUrl: String? = null,
    @SerialName("_links")
    val links: Links? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class PullRequest(
    val url: String? = null,
    val id: Long? = null,
    val number: Int? = null,
    val head: PRBranch? = null,
    val base: PRBranch? = null
)

@Serializable
data class PRBranch(
    val ref: String? = null,
    val sha: String? = null,
    val repo: RepoShort? = null
)

@Serializable
data class RepoShort(
    val id: Long? = null,
    val url: String? = null,
    val name: String? = null
)

@Serializable
data class UserSummary(
    val login: String,
    val id: Long,
    @SerialName("node_id")
    val nodeId: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("gravatar_id")
    val gravatarId: String? = null,
    val url: String? = null,
    @SerialName("html_url")
    val htmlUrl: String? = null,
    @SerialName("followers_url")
    val followersUrl: String? = null,
    @SerialName("following_url")
    val followingUrl: String? = null,
    @SerialName("gists_url")
    val gistsUrl: String? = null,
    @SerialName("starred_url")
    val starredUrl: String? = null,
    @SerialName("subscriptions_url")
    val subscriptionsUrl: String? = null,
    @SerialName("organizations_url")
    val organizationsUrl: String? = null,
    @SerialName("repos_url")
    val reposUrl: String? = null,
    @SerialName("events_url")
    val eventsUrl: String? = null,
    @SerialName("received_events_url")
    val receivedEventsUrl: String? = null,
    val type: String? = null,
    @SerialName("user_view_type")
    val userViewType: String? = null,
    @SerialName("site_admin")
    val siteAdmin: Boolean? = null
)

@Serializable
data class Issue(
    val url: String? = null,
    @SerialName("repository_url")
    val repositoryUrl: String? = null,
    @SerialName("labels_url")
    val labelsUrl: String? = null,
    @SerialName("comments_url")
    val commentsUrl: String? = null,
    @SerialName("events_url")
    val eventsUrl: String? = null,
    @SerialName("html_url")
    val htmlUrl: String? = null,
    val id: Long? = null,
    @SerialName("node_id")
    val nodeId: String? = null,
    val number: Int? = null,
    val title: String? = null,
    val user: UserSummary? = null,
    val labels: List<Label>? = null,
    val state: String? = null,
    val locked: Boolean? = null,
    val assignee: JsonElement? = null,
    val assignees: List<JsonElement>? = null,
    val milestone: JsonElement? = null,
    val comments: Int? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("closed_at")
    val closedAt: String? = null,
    val draft: Boolean? = null,
    val body: String? = null,
    @SerialName("pull_request")
    val pullRequestRef: IssuePullRequestRef? = null,
    val reactions: Reactions? = null
)

@Serializable
data class IssuePullRequestRef(
    val url: String? = null,
    @SerialName("html_url")
    val htmlUrl: String? = null,
    @SerialName("diff_url")
    val diffUrl: String? = null,
    @SerialName("patch_url")
    val patchUrl: String? = null,
    @SerialName("merged_at")
    val mergedAt: String? = null
)

@Serializable
data class Comment(
    val url: String? = null,
    @SerialName("pull_request_review_id")
    val pullRequestReviewId: Long? = null,
    val id: Long? = null,
    @SerialName("node_id")
    val nodeId: String? = null,
    @SerialName("diff_hunk")
    val diffHunk: String? = null,
    val path: String? = null,
    @SerialName("commit_id")
    val commitId: String? = null,
    val user: UserSummary? = null,
    val body: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("html_url")
    val htmlUrl: String? = null,
    @SerialName("pull_request_url")
    val pullRequestUrl: String? = null,
    @SerialName("_links")
    val links: Links? = null,
    val inReplyToId: Long? = null,
    @SerialName("original_position")
    val originalPosition: Int? = null,
    val position: Int? = null,
    @SerialName("subject_type")
    val subjectType: String? = null,
    val reactions: Reactions? = null
)

@Serializable
data class Label(
    val id: Long? = null,
    @SerialName("node_id")
    val nodeId: String? = null,
    val url: String? = null,
    val name: String? = null,
    val color: String? = null,
    val description: String? = null,
    val `default`: Boolean? = null
)

@Serializable
data class Links(
    val self: Href? = null,
    val html: Href? = null,
    @SerialName("pull_request")
    val pullRequest: Href? = null
)

@Serializable
data class Href(
    val href: String
)

@Serializable
data class Reactions(
    val url: String? = null,
    @SerialName("total_count")
    val totalCount: Int? = null,
    @SerialName("+1")
    val plusOne: Int? = null,
    @SerialName("-1")
    val minusOne: Int? = null,
    val laugh: Int? = null,
    val hooray: Int? = null,
    val confused: Int? = null,
    val heart: Int? = null,
    val rocket: Int? = null,
    val eyes: Int? = null
)