package com.shuckler.app.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.shuckler.app.ui.LibraryScreen
import com.shuckler.app.ui.PlayerScreen
import com.shuckler.app.ui.SearchScreen

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    data object Search : Screen("search", "Search", Icons.Default.Search)
    data object Library : Screen("library", "Library", Icons.Default.LibraryMusic)
    data object Player : Screen("player", "Player", Icons.Default.PlayArrow)
}

private val tabOrder = listOf(Screen.Search, Screen.Library, Screen.Player)

@Composable
fun ShucklerNavGraph(modifier: Modifier = Modifier) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Search) }

    androidx.compose.material3.Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                tabOrder.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen }
                    )
                }
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentScreen,
            modifier = Modifier.padding(innerPadding),
            transitionSpec = {
                val fromIndex = tabOrder.indexOf(initialState)
                val toIndex = tabOrder.indexOf(targetState)
                val direction = if (toIndex > fromIndex) 1 else -1
                slideInHorizontally(
                    animationSpec = tween(durationMillis = 280),
                    initialOffsetX = { fullWidth -> direction * fullWidth }
                ) togetherWith slideOutHorizontally(
                    animationSpec = tween(durationMillis = 280),
                    targetOffsetX = { fullWidth -> -direction * fullWidth }
                )
            },
            label = "tab_slide"
        ) { screen ->
            when (screen) {
                Screen.Search -> SearchScreen()
                Screen.Library -> LibraryScreen()
                Screen.Player -> PlayerScreen()
            }
        }
    }
}
