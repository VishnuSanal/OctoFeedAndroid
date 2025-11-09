package com.vishnu.octofeed.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
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
    val uriHandler = LocalUriHandler.current

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
                    .clickable {
                        uriHandler.openUri("https://github.com/${event.actor.login}")
                    }
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
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clickable {
                                uriHandler.openUri("https://github.com/${event.actor.login}")
                            },
                        textDecoration = TextDecoration.Underline
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
                EventDescriptionText(event = event, uriHandler = uriHandler)

                Spacer(modifier = Modifier.height(6.dp))

                // Repository name with icon/chip style
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable {
                            uriHandler.openUri("https://github.com/${event.repo.name}")
                        }
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

@Composable
fun EventDescriptionText(event: GitHubEvent, uriHandler: androidx.compose.ui.platform.UriHandler) {
    val repoName = event.repo.name

    when (event.type) {
        "PushEvent" -> {
            val size = event.payload?.size ?: 0
            val branch = event.payload?.ref?.removePrefix("refs/heads/") ?: "main"
            val commits = if (size == 1) "commit" else "commits"
            Text(
                text = "🔨 Pushed $size $commits to $branch",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$emoji ${actionText.capitalize()} PR ",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
                if (number != null) {
                    Text(
                        text = "#$number",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            uriHandler.openUri("https://github.com/$repoName/pull/$number")
                        },
                        textDecoration = TextDecoration.Underline
                    )
                }
                if (title != null) {
                    Text(
                        text = ": ${title.take(60)}${if (title.length > 60) "..." else ""}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$emoji ${action.capitalize()} PR ",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
                if (number != null) {
                    Text(
                        text = "#$number",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            uriHandler.openUri("https://github.com/$repoName/pull/$number")
                        },
                        textDecoration = TextDecoration.Underline
                    )
                }
            }
        }

        "PullRequestReviewCommentEvent" -> {
            val number = event.payload?.pullRequest?.number
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💬 Commented on PR ",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
                if (number != null) {
                    Text(
                        text = "#$number",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            uriHandler.openUri("https://github.com/$repoName/pull/$number")
                        },
                        textDecoration = TextDecoration.Underline
                    )
                    Text(
                        text = " review",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$emoji ${action.capitalize()} issue ",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
                if (number != null) {
                    Text(
                        text = "#$number",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            uriHandler.openUri("https://github.com/$repoName/issues/$number")
                        },
                        textDecoration = TextDecoration.Underline
                    )
                }
                if (title != null) {
                    Text(
                        text = ": ${title.take(60)}${if (title.length > 60) "..." else ""}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        "IssueCommentEvent" -> {
            val number = event.payload?.issue?.number
            val commentBody = event.payload?.comment?.body
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💬 Commented on issue ",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
                if (number != null) {
                    Text(
                        text = "#$number",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            uriHandler.openUri("https://github.com/$repoName/issues/$number")
                        },
                        textDecoration = TextDecoration.Underline
                    )
                }
                val preview = commentBody?.take(80)?.replace("\n", " ")
                if (preview != null) {
                    Text(
                        text = ": \"$preview${if (commentBody.length > 80) "...\"" else "\""}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        "WatchEvent" -> {
            Text(
                text = "⭐ Starred the repository",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }

        "ForkEvent" -> {
            val forkee = event.payload?.forkee
            if (forkee != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🍴 Forked to ",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                    Text(
                        text = "${forkee.owner?.login}/${forkee.name}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            uriHandler.openUri("https://github.com/${forkee.owner?.login}/${forkee.name}")
                        },
                        textDecoration = TextDecoration.Underline
                    )
                }
            } else {
                Text(
                    text = "🍴 Forked the repository",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
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
            if (ref != null && refType == "branch") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$emoji Created $refType \"",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                    Text(
                        text = ref,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            uriHandler.openUri("https://github.com/$repoName/tree/$ref")
                        },
                        textDecoration = TextDecoration.Underline
                    )
                    Text(
                        text = "\"",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            } else if (ref != null && refType == "tag") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$emoji Created $refType \"",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                    Text(
                        text = ref,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            uriHandler.openUri("https://github.com/$repoName/releases/tag/$ref")
                        },
                        textDecoration = TextDecoration.Underline
                    )
                    Text(
                        text = "\"",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            } else if (ref != null) {
                Text(
                    text = "$emoji Created $refType \"$ref\"",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            } else {
                Text(
                    text = "$emoji Created $refType",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
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
            Text(
                text = if (ref != null) "$emoji Deleted $refType \"$ref\"" else "$emoji Deleted $refType",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }

        "PublicEvent" -> {
            Text(
                text = "🌍 Made repository public",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }

        "MemberEvent" -> {
            val action = event.payload?.action ?: "added"
            val member = event.payload?.member
            if (member != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "👥 ${action.capitalize()} ",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                    Text(
                        text = "@${member.login}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            uriHandler.openUri("https://github.com/${member.login}")
                        },
                        textDecoration = TextDecoration.Underline
                    )
                    Text(
                        text = " as collaborator",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            } else {
                Text(
                    text = "👥 ${action.capitalize()} a collaborator",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        }

        "ReleaseEvent" -> {
            val action = event.payload?.action ?: "published"
            val release = event.payload?.release
            if (release?.name != null || release?.tagName != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🚀 ${action.capitalize()} release ",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                    Text(
                        text = "\"${release.name ?: release.tagName}\"",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            val tag = release.tagName ?: ""
                            uriHandler.openUri("https://github.com/$repoName/releases/tag/$tag")
                        },
                        textDecoration = TextDecoration.Underline
                    )
                }
            } else {
                Text(
                    text = "🚀 ${action.capitalize()} a release",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        }

        else -> {
            Text(
                text = "📌 ${event.type.replace("Event", "")}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }
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


