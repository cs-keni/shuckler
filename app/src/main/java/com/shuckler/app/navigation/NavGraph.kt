package com.shuckler.app.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shuckler.app.download.LocalDownloadManager
import com.shuckler.app.playlist.Playlist
import com.shuckler.app.ui.AnalyticsScreen
import com.shuckler.app.ui.LocalOnWifiOnlyBlocked
import com.shuckler.app.ui.LocalSnackbarHostState
import com.shuckler.app.ui.HomeScreen
import com.shuckler.app.ui.LibraryScreen
import com.shuckler.app.ui.MiniPlayerBar
import com.shuckler.app.ui.PlayerScreen
import com.shuckler.app.ui.SearchScreen
import com.shuckler.app.ui.EqualizerDialog
import com.shuckler.app.ui.OnboardingScreen
import com.shuckler.app.accessibility.LocalAccessibilityPreferences
import com.shuckler.app.ui.SettingsDialog
import com.shuckler.app.player.LocalMusicServiceConnection
import com.shuckler.app.player.PlayerViewModel
import com.shuckler.app.player.QueueItem
import kotlinx.coroutines.launch

sealed class Screen(
    val route: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    data object Home : Screen("home", "Home", Icons.Default.Home)
    data object Search : Screen("search", "Search", Icons.Default.Search)
    data object Library : Screen("library", "Library", Icons.Default.LibraryMusic)
    data object Analytics : Screen("analytics", "Analytics", Icons.Default.Analytics)
}

private val tabOrder = listOf(Screen.Home, Screen.Search, Screen.Library, Screen.Analytics)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShucklerNavGraph(modifier: Modifier = Modifier) {
    val downloadManager = LocalDownloadManager.current
    val initialScreen = when (downloadManager.defaultTab) {
        "search" -> Screen.Search
        "library" -> Screen.Library
        "analytics" -> Screen.Analytics
        else -> Screen.Home
    }
    var currentScreen by remember { mutableStateOf<Screen>(initialScreen) }
    var previousScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var showLibrarySheet by remember { mutableStateOf(initialScreen == Screen.Library) }
    var libraryScrollIndex by remember { mutableStateOf(0) }
    var libraryScrollOffset by remember { mutableStateOf(0) }
    var searchScrollIndex by remember { mutableStateOf(0) }
    var searchScrollOffset by remember { mutableStateOf(0) }
    var showPlayerSheet by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showEqualizerDialog by remember { mutableStateOf(false) }
    var showTutorial by remember { mutableStateOf(false) }
    var selectedPlaylistToOpen by remember { mutableStateOf<Playlist?>(null) }
    var pendingSearchQuery by remember { mutableStateOf<String?>(null) }

    val viewModel: PlayerViewModel = viewModel(
        factory = PlayerViewModel.Factory(
            LocalContext.current,
            LocalMusicServiceConnection.current
        )
    )
    val queueItems by viewModel.queueItems.collectAsState(initial = emptyList<QueueItem>())
    val durationMs by viewModel.durationMs.collectAsState(initial = 0L)
    val hasActivePlayback = queueItems.isNotEmpty() || durationMs > 0L

    val onMiniPlayerTap: () -> Unit = { showPlayerSheet = true }
    val onPlayerCollapse: () -> Unit = { showPlayerSheet = false }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val onWifiOnlyBlocked: () -> Unit = {
        scope.launch { snackbarHostState.showSnackbar("Downloads are Wi‑Fi only") }
    }
    val sleepTimerRemainingMs by viewModel.sleepTimerRemainingMs.collectAsState(initial = null)
    val musicService by LocalMusicServiceConnection.current.service.collectAsState(initial = null)

    val accessibilityPrefs = LocalAccessibilityPreferences.current
    if (showSettingsDialog) {
        SettingsDialog(
            autoDeleteAfterPlayback = downloadManager.autoDeleteAfterPlayback,
            onAutoDeleteChange = { downloadManager.autoDeleteAfterPlayback = it },
            crossfadeDurationMs = downloadManager.crossfadeDurationMs,
            onCrossfadeChange = { downloadManager.crossfadeDurationMs = it },
            downloadQuality = downloadManager.downloadQuality,
            onDownloadQualityChange = { downloadManager.downloadQuality = it },
            sleepTimerRemainingMs = sleepTimerRemainingMs,
            onStartSleepTimer = { durationMs, endOfTrack -> viewModel.startSleepTimer(durationMs, endOfTrack) },
            onCancelSleepTimer = { viewModel.cancelSleepTimer() },
            sleepTimerFadeLastMinute = downloadManager.sleepTimerFadeLastMinute,
            onSleepTimerFadeChange = { downloadManager.sleepTimerFadeLastMinute = it },
            defaultTab = downloadManager.defaultTab,
            onDefaultTabChange = { downloadManager.defaultTab = it },
            wifiOnlyDownloads = downloadManager.wifiOnlyDownloads,
            onWifiOnlyDownloadsChange = { downloadManager.wifiOnlyDownloads = it },
            onEqualizerClick = { showEqualizerDialog = true },
            onShowTutorial = {
                showSettingsDialog = false
                showTutorial = true
            },
            reduceMotion = accessibilityPrefs.reduceMotion,
            onReduceMotionChange = { accessibilityPrefs.reduceMotion = it },
            highContrast = accessibilityPrefs.highContrast,
            onHighContrastChange = { accessibilityPrefs.highContrast = it },
            onDismiss = { showSettingsDialog = false }
        )
    }
    if (showEqualizerDialog) {
        EqualizerDialog(
            service = musicService,
            onDismiss = { showEqualizerDialog = false }
        )
    }

    if (showPlayerSheet) {
        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { true }
        )
        ModalBottomSheet(
            onDismissRequest = { showPlayerSheet = false },
            sheetState = sheetState,
            containerColor = Color(0xFF121212),
            dragHandle = null
        ) {
            PlayerScreen(
                onCollapse = onPlayerCollapse,
                fromMiniPlayer = true,
                viewModel = viewModel
            )
        }
    }

    if (showLibrarySheet) {
        val librarySheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { true }
        )
        ModalBottomSheet(
            onDismissRequest = {
                showLibrarySheet = false
                currentScreen = previousScreen
            },
            sheetState = librarySheetState,
            containerColor = Color(0xFF121212)
        ) {
            LibraryScreen(
                initialPlaylistToOpen = selectedPlaylistToOpen,
                onClearInitialPlaylist = { selectedPlaylistToOpen = null },
                onSettingsClick = { showSettingsDialog = true },
                onOpenSearch = {
                    showLibrarySheet = false
                    currentScreen = Screen.Search
                },
                savedScrollIndex = libraryScrollIndex,
                savedScrollOffset = libraryScrollOffset,
                onSaveScrollPosition = { idx, off ->
                    libraryScrollIndex = idx
                    libraryScrollOffset = off
                },
                isSheetMode = true,
                viewModel = viewModel
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    androidx.compose.material3.Scaffold(
        modifier = modifier,
        containerColor = Color(0xFF121212),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column(
                modifier = Modifier.background(Color(0xCC121212))
            ) {
                if (hasActivePlayback) {
                    MiniPlayerBar(onTap = onMiniPlayerTap, viewModel = viewModel)
                }
                NavigationBar(
                    containerColor = Color.Transparent,
                    contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 0.dp
                ) {
                    tabOrder.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentScreen == screen,
                            onClick = {
                                if (screen == Screen.Library) {
                                    if (currentScreen != Screen.Library) previousScreen = currentScreen
                                    currentScreen = Screen.Library
                                    showLibrarySheet = true
                                } else {
                                    currentScreen = screen
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        CompositionLocalProvider(
            LocalOnWifiOnlyBlocked provides onWifiOnlyBlocked,
            LocalSnackbarHostState provides snackbarHostState
        ) {
        val effectiveScreen = if (currentScreen == Screen.Library) previousScreen else currentScreen
        AnimatedContent(
            targetState = effectiveScreen,
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
                Screen.Home -> HomeScreen(
                    onPlaylistSelected = {
                        selectedPlaylistToOpen = it
                        previousScreen = Screen.Home
                        currentScreen = Screen.Library
                        showLibrarySheet = true
                    },
                    onSearchQuerySelected = {
                        pendingSearchQuery = it
                        currentScreen = Screen.Search
                    },
                    onSettingsClick = { showSettingsDialog = true },
                    viewModel = viewModel
                )
                Screen.Search -> SearchScreen(
                    onSettingsClick = { showSettingsDialog = true },
                    initialQuery = pendingSearchQuery,
                    onInitialQueryConsumed = { pendingSearchQuery = null },
                    savedScrollOffset = searchScrollOffset,
                    onSaveScrollPosition = { searchScrollOffset = it },
                    viewModel = viewModel
                )
                Screen.Library -> LibraryScreen(
                    initialPlaylistToOpen = selectedPlaylistToOpen,
                    onClearInitialPlaylist = { selectedPlaylistToOpen = null },
                    onSettingsClick = { showSettingsDialog = true },
                    onOpenSearch = { currentScreen = Screen.Search },
                    savedScrollIndex = libraryScrollIndex,
                    savedScrollOffset = libraryScrollOffset,
                    onSaveScrollPosition = { idx, off ->
                        libraryScrollIndex = idx
                        libraryScrollOffset = off
                    }
                )
                Screen.Analytics -> AnalyticsScreen(onSettingsClick = { showSettingsDialog = true })
            }
        }
        }
    }
        if (showTutorial) {
            OnboardingScreen(onComplete = { showTutorial = false })
        }
    }
}
