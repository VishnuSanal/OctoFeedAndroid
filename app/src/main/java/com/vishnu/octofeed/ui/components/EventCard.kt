package com.vishnu.octofeed.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vishnu.octofeed.data.models.FeedEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun EventCard(event: FeedEvent, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            AsyncImage(
                model = event.actor.avatarUrl,
                contentDescription = "${event.actor.login} avatar",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Event-specific content
                when (event) {
                    is FeedEvent.StarEvent -> StarEventContent(event)
                    is FeedEvent.ForkEvent -> ForkEventContent(event)
                    is FeedEvent.CreateRepoEvent -> CreateRepoEventContent(event)
                    is FeedEvent.ReleaseEvent -> ReleaseEventContent(event)
                    is FeedEvent.FollowEvent -> FollowEventContent(event)
                }

                // Timestamp
                Text(
                    text = formatTimestamp(event.timestamp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Icon
            EventIcon(event)
        }
    }
}

@Composable
private fun StarEventContent(event: FeedEvent.StarEvent) {
    Text(
        text = buildString {
            append(event.actor.login)
            append(" starred ")
        },
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
        text = event.repo.name,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun ForkEventContent(event: FeedEvent.ForkEvent) {
    Text(
        text = "${event.actor.login} forked",
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
        text = event.repo.name,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
    event.forkedRepo?.let {
        Text(
            text = "to $it",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CreateRepoEventContent(event: FeedEvent.CreateRepoEvent) {
    Text(
        text = "${event.actor.login} created repository",
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
        text = event.repo.name,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun ReleaseEventContent(event: FeedEvent.ReleaseEvent) {
    Text(
        text = "${event.actor.login} released ${event.tagName} in",
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
        text = event.repo.name,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
    if (event.releaseName != event.tagName) {
        Text(
            text = event.releaseName,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FollowEventContent(event: FeedEvent.FollowEvent) {
    Text(
        text = "${event.actor.login} started following you",
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun EventIcon(event: FeedEvent) {
    val (icon, tint) = when (event) {
        is FeedEvent.StarEvent -> Icons.Default.Star to MaterialTheme.colorScheme.primary
        is FeedEvent.ForkEvent -> Icons.Default.Share to MaterialTheme.colorScheme.secondary
        is FeedEvent.CreateRepoEvent -> Icons.Default.Add to MaterialTheme.colorScheme.tertiary
        is FeedEvent.ReleaseEvent -> Icons.Default.Settings to MaterialTheme.colorScheme.primary
        is FeedEvent.FollowEvent -> Icons.Default.Person to MaterialTheme.colorScheme.secondary
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
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

