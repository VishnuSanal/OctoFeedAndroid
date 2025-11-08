package com.vishnu.octofeed

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.vishnu.octofeed.auth.TokenManager
import com.vishnu.octofeed.ui.screens.HomeScreen
import com.vishnu.octofeed.ui.screens.LoginScreen
import com.vishnu.octofeed.ui.theme.OctoFeedTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OctoFeedTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    OctoFeedApp(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun OctoFeedApp(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    var isLoggedIn by remember { mutableStateOf(tokenManager.isLoggedIn()) }

    // Recheck login status when composition is resumed
    LaunchedEffect(Unit) {
        isLoggedIn = tokenManager.isLoggedIn()
    }

    if (isLoggedIn) {
        HomeScreen(
            modifier = modifier,
            onLogout = {
                tokenManager.clearAuth()
                isLoggedIn = false
            }
        )
    } else {
        LoginScreen(
            modifier = modifier
        )
    }
}