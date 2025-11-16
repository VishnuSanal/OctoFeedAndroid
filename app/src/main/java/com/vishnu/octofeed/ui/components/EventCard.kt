package com.vishnu.octofeed.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vishnu.octofeed.data.models.FeedEvent
import com.vishnu.octofeed.data.models.RepoDetails
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun EventCard(
    event: FeedEvent,
    modifier: Modifier = Modifier,
    onStarToggle: ((String, Boolean, (Boolean) -> Unit) -> Unit)? = null,
    onCheckStarStatus: ((String, (Boolean) -> Unit) -> Unit)? = null
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar with better Material 3 styling
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(event.actor.avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "${event.actor.login} avatar",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                "https://github.com/${event.actor.login}".toUri()
                            )
                            context.startActivity(intent)
                        }
                )
            }

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Event-specific content
                when (event) {
                    is FeedEvent.StarEvent -> StarEventContent(
                        event,
                        onStarToggle,
                        onCheckStarStatus
                    )

                    is FeedEvent.ForkEvent -> ForkEventContent(
                        event,
                        onStarToggle,
                        onCheckStarStatus
                    )

                    is FeedEvent.CreateRepoEvent -> CreateRepoEventContent(
                        event,
                        onStarToggle,
                        onCheckStarStatus
                    )

                    is FeedEvent.ReleaseEvent -> ReleaseEventContent(
                        event,
                        onStarToggle,
                        onCheckStarStatus
                    )

                    is FeedEvent.FollowEvent -> FollowEventContent(event)
                }

                // Timestamp with Material 3 label style
                Text(
                    text = formatTimestamp(event.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StarEventContent(
    event: FeedEvent.StarEvent,
    onStarToggle: ((String, Boolean, (Boolean) -> Unit) -> Unit)? = null,
    onCheckStarStatus: ((String, (Boolean) -> Unit) -> Unit)? = null
) {
    val context = LocalContext.current

    Text(
        text = buildString {
            append(event.actor.login)
            append(" starred ")
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
        text = event.repo.name,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clickable {
            val intent = Intent(Intent.ACTION_VIEW, "https://github.com/${event.repo.name}".toUri())
            context.startActivity(intent)
        }
    )
    event.repoDetails?.let { details ->
        RepoDetailsContent(details, onStarToggle, onCheckStarStatus)
    }
}

@Composable
private fun ForkEventContent(
    event: FeedEvent.ForkEvent,
    onStarToggle: ((String, Boolean, (Boolean) -> Unit) -> Unit)? = null,
    onCheckStarStatus: ((String, (Boolean) -> Unit) -> Unit)? = null
) {
    val context = LocalContext.current

    Text(
        text = "${event.actor.login} forked",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
        text = event.repo.name,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clickable {
            val intent = Intent(Intent.ACTION_VIEW, "https://github.com/${event.repo.name}".toUri())
            context.startActivity(intent)
        }
    )
    event.forkedRepo?.let {
        Text(
            text = "to $it",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    event.repoDetails?.let { details ->
        RepoDetailsContent(details, onStarToggle, onCheckStarStatus)
    }
}

@Composable
private fun CreateRepoEventContent(
    event: FeedEvent.CreateRepoEvent,
    onStarToggle: ((String, Boolean, (Boolean) -> Unit) -> Unit)? = null,
    onCheckStarStatus: ((String, (Boolean) -> Unit) -> Unit)? = null
) {
    val context = LocalContext.current

    Text(
        text = "${event.actor.login} created repository",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
        text = event.repo.name,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clickable {
            val intent = Intent(Intent.ACTION_VIEW, "https://github.com/${event.repo.name}".toUri())
            context.startActivity(intent)
        }
    )
    event.repoDetails?.let { details ->
        RepoDetailsContent(details, onStarToggle, onCheckStarStatus)
    }
}

@Composable
private fun ReleaseEventContent(
    event: FeedEvent.ReleaseEvent,
    onStarToggle: ((String, Boolean, (Boolean) -> Unit) -> Unit)? = null,
    onCheckStarStatus: ((String, (Boolean) -> Unit) -> Unit)? = null
) {
    val context = LocalContext.current

    Text(
        text = "${event.actor.login} released ${event.tagName} in",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
        text = event.repo.name,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clickable {
            val intent = Intent(Intent.ACTION_VIEW, "https://github.com/${event.repo.name}".toUri())
            context.startActivity(intent)
        }
    )
    if (event.releaseName != event.tagName) {
        Text(
            text = event.releaseName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    event.repoDetails?.let { details ->
        RepoDetailsContent(details, onStarToggle, onCheckStarStatus)
    }
}

@Composable
private fun FollowEventContent(event: FeedEvent.FollowEvent) {
    val context = LocalContext.current

    Text(
        text = "${event.actor.login} started following you",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clickable {
            val intent = Intent(
                Intent.ACTION_VIEW,
                "https://github.com/${event.actor.login}".toUri()
            )
            context.startActivity(intent)
        }
    )
}

@Composable
private fun RepoDetailsContent(
    details: RepoDetails,
    onStarToggle: ((String, Boolean, (Boolean) -> Unit) -> Unit)? = null,
    onCheckStarStatus: ((String, (Boolean) -> Unit) -> Unit)? = null
) {
    var isStarred by remember { mutableStateOf<Boolean?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Check star status on mount
    LaunchedEffect(details.fullName) {
        onCheckStarStatus?.invoke(details.fullName) { starred ->
            isStarred = starred
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header row with description and star button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Description
                if (details.description.isNullOrBlank()) {
                    Spacer(Modifier.weight(1f))
                } else {
                    Text(
                        text = details.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }

                // Star button
                if (onStarToggle != null) {
                    IconButton(
                        onClick = {
                            isStarred?.let { currentState ->
                                isLoading = true
                                onStarToggle(details.fullName, currentState) { newState ->
                                    isStarred = newState
                                    isLoading = false
                                }
                            }
                        },
                        enabled = isStarred != null && !isLoading,
                        modifier = Modifier.size(40.dp)
                    ) {
                        when {
                            isLoading -> CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            isStarred == true -> Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "Unstar",
                                tint = MaterialTheme.colorScheme.primary
                            )

                            else -> Icon(
                                imageVector = Icons.Outlined.Star,
                                contentDescription = "Star",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Language
                details.language?.let { lang ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(getLanguageColor(lang))
                        )
                        Text(
                            text = lang,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Stars
                if (details.stargazersCount > 0) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Stars",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = formatCount(details.stargazersCount),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Forks
                if (details.forksCount > 0) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Forks",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = formatCount(details.forksCount),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Topics
            if (details.topics.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    details.topics.take(3).forEach { topic ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = topic,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (details.topics.size > 3) {
                        Text(
                            text = "+${details.topics.size - 3}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun formatCount(count: Int): String {
    return when {
        count >= 1000000 -> String.format("%.1fM", count / 1000000.0)
        count >= 1000 -> String.format("%.1fk", count / 1000.0)
        else -> count.toString()
    }
}

private fun getLanguageColor(language: String): androidx.compose.ui.graphics.Color {
    // Common language colors based on GitHub's color scheme
    return when (language.lowercase()) {
        "kotlin" -> androidx.compose.ui.graphics.Color(0xFFA97BFF)
        "java" -> androidx.compose.ui.graphics.Color(0xFFB07219)
        "javascript" -> androidx.compose.ui.graphics.Color(0xFFF1E05A)
        "typescript" -> androidx.compose.ui.graphics.Color(0xFF2B7489)
        "python" -> androidx.compose.ui.graphics.Color(0xFF3572A5)
        "go" -> androidx.compose.ui.graphics.Color(0xFF00ADD8)
        "rust" -> androidx.compose.ui.graphics.Color(0xFFDEA584)
        "swift" -> androidx.compose.ui.graphics.Color(0xFFFFAC45)
        "c++" -> androidx.compose.ui.graphics.Color(0xFFF34B7D)
        "c" -> androidx.compose.ui.graphics.Color(0xFF555555)
        "c#" -> androidx.compose.ui.graphics.Color(0xFF178600)
        "ruby" -> androidx.compose.ui.graphics.Color(0xFF701516)
        "php" -> androidx.compose.ui.graphics.Color(0xFF4F5D95)
        "dart" -> androidx.compose.ui.graphics.Color(0xFF00B4AB)
        "shell" -> androidx.compose.ui.graphics.Color(0xFF89E051)
        "html" -> androidx.compose.ui.graphics.Color(0xFFE34C26)
        "css" -> androidx.compose.ui.graphics.Color(0xFF563D7C)
        else -> androidx.compose.ui.graphics.Color(0xFF858585)
    }
}

private fun formatTimestamp(timestamp: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val date = sdf.parse(timestamp) ?: return timestamp

        val now = Date()
        val diffMillis = now.time - date.time
        val minutes = diffMillis / (1000 * 60)
        val hours = diffMillis / (1000 * 60 * 60)
        val days = diffMillis / (1000 * 60 * 60 * 24)

        when {
            minutes < 1 -> "just now"
            minutes < 60 -> "$minutes minute${if (minutes != 1L) "s" else ""} ago"
            hours < 24 -> "$hours hour${if (hours != 1L) "s" else ""} ago"
            days < 7 -> "$days day${if (days != 1L) "s" else ""} ago"
            else -> {
                val displayFormat = SimpleDateFormat("MMM dd", Locale.US)
                displayFormat.format(date)
            }
        }
    } catch (e: Exception) {
        timestamp
    }
}

