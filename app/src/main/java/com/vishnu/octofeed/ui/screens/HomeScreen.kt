package com.vishnu.octofeed.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = event.actor.login,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatTimestamp(event.createdAt),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = getEventDescription(event),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

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

fun getEventDescription(event: GitHubEvent): String {
    return when (event.type) {
        "PushEvent" -> {
            val size = event.payload?.size ?: 0
            val commits = if (size == 1) "commit" else "commits"
            "Pushed $size $commits"
        }

        "PullRequestEvent" -> {
            val action = event.payload?.action ?: "updated"
            val number = event.payload?.number ?: event.payload?.pullRequest?.number
            if (number != null) {
                "Pull request #$number $action"
            } else {
                "Pull request $action"
            }
        }

        "PullRequestReviewEvent" -> {
            val state = event.payload?.review?.state ?: "submitted"
            val action = when (state) {
                "approved" -> "approved"
                "commented" -> "commented on"
                "changes_requested" -> "requested changes on"
                else -> "reviewed"
            }
            "Pull request $action"
        }

        "PullRequestReviewCommentEvent" -> {
            val action = event.payload?.action ?: "commented"
            "Pull request review comment $action"
        }

        "IssuesEvent" -> {
            val action = event.payload?.action ?: "updated"
            val number = event.payload?.number ?: event.payload?.issue?.number
            if (number != null) {
                "Issue #$number $action"
            } else {
                "Issue $action"
            }
        }

        "IssueCommentEvent" -> {
            val action = event.payload?.action ?: "created"
            "$action a comment on an issue"
        }

        "WatchEvent" -> "Starred the repository"
        "ForkEvent" -> "Forked the repository"
        "CreateEvent" -> {
            val refType = event.payload?.refType ?: "repository"
            "Created $refType" + (event.payload?.ref?.let { " $it" } ?: "")
        }

        "DeleteEvent" -> {
            val refType = event.payload?.refType ?: "branch"
            "Deleted $refType" + (event.payload?.ref?.let { " $it" } ?: "")
        }

        "PublicEvent" -> "Made repository public"
        "MemberEvent" -> {
            val action = event.payload?.action ?: "added"
            "$action collaborator"
        }

        "ReleaseEvent" -> {
            val action = event.payload?.action ?: "published"
            "$action a release"
        }

        else -> event.type.replace("Event", "")
    }
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


