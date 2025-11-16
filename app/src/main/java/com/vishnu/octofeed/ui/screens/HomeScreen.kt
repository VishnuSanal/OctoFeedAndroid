package com.vishnu.octofeed.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.vishnu.octofeed.auth.TokenManager
import com.vishnu.octofeed.ui.viewmodels.FeedViewModel

@Composable
fun HomeScreen(
    modifier: Modifier,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val tokenManager = TokenManager(context)
    val accessToken = tokenManager.getAccessToken() ?: return
    val userName = tokenManager.getUserLogin() ?: "User"

    val viewModel = remember {
        FeedViewModel(
            accessToken = accessToken,
            username = userName
        )
    }

    FeedScreen(
        viewModel = viewModel,
        userName = userName,
        onLogout = onLogout,
        modifier = modifier
    )
}

