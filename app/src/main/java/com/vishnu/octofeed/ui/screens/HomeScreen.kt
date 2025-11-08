package com.vishnu.octofeed.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vishnu.octofeed.auth.TokenManager

@Composable
fun HomeScreen(
    modifier: Modifier,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val tokenManager = TokenManager(context)
    val userName = tokenManager.getUserName() ?: tokenManager.getUserLogin() ?: "User"

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Header
        Text(
            text = "Welcome, $userName! 👋",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "You're successfully logged in to OctoFeed!",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "User Info",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Username: ${tokenManager.getUserLogin() ?: "N/A"}")
                Text(text = "Name: ${tokenManager.getUserName() ?: "N/A"}")
                if (tokenManager.getAccessToken() != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "✓ Authenticated",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Logout Button
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text(
                text = "Logout",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

