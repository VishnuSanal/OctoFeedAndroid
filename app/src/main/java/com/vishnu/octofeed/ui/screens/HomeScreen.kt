package com.vishnu.octofeed.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vishnu.octofeed.auth.TokenManager
import com.vishnu.octofeed.data.GitHubEvent
import com.vishnu.octofeed.ui.viewmodel.FeedUiState
import com.vishnu.octofeed.ui.viewmodel.FeedViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val tokenManager = TokenManager(context)

    val userName = tokenManager.getUserLogin() ?: "User"
    val accessToken = tokenManager.getAccessToken() ?: ""

    val viewModel = remember { FeedViewModel() }
    val uiState by viewModel.uiState.collectAsState()

    // Load feed when screen is first displayed
    LaunchedEffect(userName, accessToken) {
        if (accessToken.isNotEmpty()) {
            viewModel.loadFeed(userName, accessToken)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "OctoFeed",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("Logout", color = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is FeedUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is FeedUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "Error loading feed",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.refresh(userName, accessToken) }) {
                            Text("Retry")
                        }
                    }
                }
            }

            is FeedUiState.Success -> {
                if (state.events.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = "No events yet",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Start following users to see their activity",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(state.events, key = { it.id }) { event ->
                            EventCard(event = event)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EventCard(event: GitHubEvent) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // User Avatar
            AsyncImage(
                model = event.actor.avatarUrl,
                contentDescription = "Avatar of ${event.actor.login}",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Event Content
            Column(modifier = Modifier.weight(1f)) {
                // Username and time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = event.actor.login,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatTimestamp(event.createdAt),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Event description
                Text(
                    text = getEventDescription(event),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Repository name with icon/chip style
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "📦",
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = event.repo.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

fun getEventDescription(event: GitHubEvent): String {
    return when (event.type) {
        "PushEvent" -> {
            val size = event.payload?.size ?: 0
            val branch = event.payload?.ref?.removePrefix("refs/heads/") ?: "main"
            val commits = if (size == 1) "commit" else "commits"
            "🔨 Pushed $size $commits to $branch"
        }

        "PullRequestEvent" -> {
            val action = event.payload?.action ?: "updated"
            val number = event.payload?.number ?: event.payload?.pullRequest?.number
            val title = event.payload?.pullRequest?.title
            val emoji = when (action) {
                "opened" -> "📝"
                "closed" -> if (event.payload?.pullRequest?.merged == true) "✅" else "❌"
                "reopened" -> "🔄"
                else -> "📋"
            }

            val actionText = when (action) {
                "opened" -> "opened"
                "closed" -> if (event.payload?.pullRequest?.merged == true) "merged" else "closed"
                "reopened" -> "reopened"
                else -> action
            }

            if (number != null && title != null) {
                "$emoji ${actionText.capitalize()} PR #$number: ${title.take(60)}${if (title.length > 60) "..." else ""}"
            } else if (number != null) {
                "$emoji ${actionText.capitalize()} pull request #$number"
            } else {
                "$emoji ${actionText.capitalize()} a pull request"
            }
        }

        "PullRequestReviewEvent" -> {
            val state = event.payload?.review?.state ?: "submitted"
            val number = event.payload?.pullRequest?.number
            val emoji = when (state) {
                "approved" -> "✅"
                "commented" -> "💬"
                "changes_requested" -> "🔄"
                else -> "👀"
            }
            val action = when (state) {
                "approved" -> "approved"
                "commented" -> "commented on"
                "changes_requested" -> "requested changes on"
                else -> "reviewed"
            }
            if (number != null) {
                "$emoji ${action.capitalize()} PR #$number"
            } else {
                "$emoji ${action.capitalize()} a pull request"
            }
        }

        "PullRequestReviewCommentEvent" -> {
            val number = event.payload?.pullRequest?.number
            if (number != null) {
                "💬 Commented on PR #$number review"
            } else {
                "💬 Commented on a pull request review"
            }
        }

        "IssuesEvent" -> {
            val action = event.payload?.action ?: "updated"
            val number = event.payload?.number ?: event.payload?.issue?.number
            val title = event.payload?.issue?.title
            val emoji = when (action) {
                "opened" -> "🐛"
                "closed" -> "✅"
                "reopened" -> "🔄"
                else -> "📋"
            }

            if (number != null && title != null) {
                "$emoji ${action.capitalize()} issue #$number: ${title.take(60)}${if (title.length > 60) "..." else ""}"
            } else if (number != null) {
                "$emoji ${action.capitalize()} issue #$number"
            } else {
                "$emoji ${action.capitalize()} an issue"
            }
        }

        "IssueCommentEvent" -> {
            val number = event.payload?.issue?.number
            val commentBody = event.payload?.comment?.body
            if (number != null) {
                val preview = commentBody?.take(80)?.replace("\n", " ")
                if (preview != null) {
                    "💬 Commented on issue #$number: \"$preview${if (commentBody.length > 80) "...\"" else "\""}"
                } else {
                    "💬 Commented on issue #$number"
                }
            } else {
                "💬 Commented on an issue"
            }
        }

        "WatchEvent" -> "⭐ Starred the repository"

        "ForkEvent" -> {
            val forkee = event.payload?.forkee
            if (forkee != null) {
                "🍴 Forked to ${forkee.owner?.login}/${forkee.name}"
            } else {
                "🍴 Forked the repository"
            }
        }

        "CreateEvent" -> {
            val refType = event.payload?.refType ?: "repository"
            val ref = event.payload?.ref
            val emoji = when (refType) {
                "branch" -> "🌿"
                "tag" -> "🏷️"
                else -> "📦"
            }
            if (ref != null) {
                "$emoji Created $refType \"$ref\""
            } else {
                "$emoji Created $refType"
            }
        }

        "DeleteEvent" -> {
            val refType = event.payload?.refType ?: "branch"
            val ref = event.payload?.ref
            val emoji = when (refType) {
                "branch" -> "🗑️"
                "tag" -> "🗑️"
                else -> "🗑️"
            }
            if (ref != null) {
                "$emoji Deleted $refType \"$ref\""
            } else {
                "$emoji Deleted $refType"
            }
        }

        "PublicEvent" -> "🌍 Made repository public"
        
        "MemberEvent" -> {
            val action = event.payload?.action ?: "added"
            val member = event.payload?.member
            if (member != null) {
                "👥 ${action.capitalize()} @${member.login} as collaborator"
            } else {
                "👥 ${action.capitalize()} a collaborator"
            }
        }

        "ReleaseEvent" -> {
            val action = event.payload?.action ?: "published"
            val release = event.payload?.release
            if (release?.name != null) {
                "🚀 ${action.capitalize()} release \"${release.name}\""
            } else if (release?.tagName != null) {
                "🚀 ${action.capitalize()} release ${release.tagName}"
            } else {
                "🚀 ${action.capitalize()} a release"
            }
        }

        else -> "📌 ${event.type.replace("Event", "")}"
    }
}

// Extension function for capitalize
private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

fun formatTimestamp(timestamp: String): String {
    return try {
        val instant = Instant.parse(timestamp)
        val now = Instant.now()
        val minutes = ChronoUnit.MINUTES.between(instant, now)
        val hours = ChronoUnit.HOURS.between(instant, now)
        val days = ChronoUnit.DAYS.between(instant, now)

        when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            else -> {
                val formatter = DateTimeFormatter.ofPattern("MMM d")
                    .withZone(ZoneId.systemDefault())
                formatter.format(instant)
            }
        }
    } catch (e: Exception) {
        timestamp
    }
}


