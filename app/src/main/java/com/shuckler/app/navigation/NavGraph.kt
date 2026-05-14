package com.shuckler.app.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.animateColorAsState
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.palette.graphics.Palette
import com.shuckler.app.ui.theme.Amber
import com.shuckler.app.ui.theme.Base
import com.shuckler.app.ui.theme.LocalAccentColor
import com.shuckler.app.ui.theme.Surface
import com.shuckler.app.ui.theme.Text1
import com.shuckler.app.ui.theme.Text3
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
import android.content.Intent
import android.net.Uri
import com.shuckler.app.lastfm.LocalLastFmScrobbler
import com.shuckler.app.player.LocalMusicServiceConnection
import com.shuckler.app.player.PlayerViewModel
import com.shuckler.app.player.QueueItem
import kotlinx.coroutines.launch

sealed class Screen(
    val route: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    data object Home : Screen("home", "Home", Icons.Rounded.Home)
    data object Search : Screen("search", "Search", Icons.Rounded.Search)
    data object Library : Screen("library", "Library", Icons.Rounded.LibraryMusic)
    data object Analytics : Screen("analytics", "Stats", Icons.Rounded.BarChart)
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

    // Palette extraction — drives LocalAccentColor for pill + Now Playing
    val context = LocalContext.current
    val thumbnailUrl by viewModel.currentTrackThumbnailUrl.collectAsState(initial = null)
    var extractedAccent by remember { mutableStateOf<Color>(Amber) }
    LaunchedEffect(thumbnailUrl) {
        extractedAccent = thumbnailUrl?.let { url ->
            withContext(Dispatchers.IO) {
                try {
                    val req = ImageRequest.Builder(context).data(url).allowHardware(false).build()
                    val result = context.imageLoader.execute(req)
                    (result as? SuccessResult)
                        ?.drawable
                        ?.let { it as? android.graphics.drawable.BitmapDrawable }
                        ?.bitmap
                        ?.let { bmp ->
                            Palette.from(bmp).generate()
                                .let { p -> p.vibrantSwatch ?: p.dominantSwatch }
                                ?.rgb?.let { Color(0xFF000000.toInt() or it) }
                        }
                } catch (_: Exception) { null }
            }
        } ?: Amber
    }
    val animatedAccent by animateColorAsState(
        targetValue = extractedAccent,
        animationSpec = tween(600),
        label = "accentColor"
    )

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
    val reduceMotion by accessibilityPrefs.reduceMotionFlow.collectAsState(initial = accessibilityPrefs.reduceMotion)
    val lastFmScrobbler = LocalLastFmScrobbler.current
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
            lastFmConnected = lastFmScrobbler.isConnected,
            lastFmUsername = lastFmScrobbler.username,
            lastFmConfigured = lastFmScrobbler.isConfigured,
            onLastFmConnect = {
                val url = lastFmScrobbler.getAuthUrl()
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            },
            onLastFmDisconnect = { lastFmScrobbler.disconnect() },
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
            containerColor = Base,
            dragHandle = null
        ) {
            var playerLaunched by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { playerLaunched = true }
            AnimatedVisibility(
                visible = playerLaunched,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    initialScale = 0.94f
                ) + fadeIn(animationSpec = tween(durationMillis = 180)),
                exit = ExitTransition.None
            ) {
                PlayerScreen(
                    onCollapse = onPlayerCollapse,
                    fromMiniPlayer = true,
                    viewModel = viewModel
                )
            }
        }
    }

    if (showLibrarySheet) {
        val librarySheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = false,
            confirmValueChange = { true }
        )
        ModalBottomSheet(
            onDismissRequest = {
                showLibrarySheet = false
                currentScreen = previousScreen
            },
            sheetState = librarySheetState,
            containerColor = Base
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

    CompositionLocalProvider(LocalAccentColor provides animatedAccent) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Base)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            animatedAccent.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = Offset(size.width / 2f, 0f),
                        radius = size.width * 0.85f
                    )
                )
            }
    ) {
    androidx.compose.material3.Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column(
                modifier = Modifier.background(Base.copy(alpha = 0.97f))
            ) {
                if (hasActivePlayback) {
                    MiniPlayerBar(onTap = onMiniPlayerTap, viewModel = viewModel)
                }
                NavigationBar(
                    containerColor = Color.Transparent,
                    contentColor = Text1,
                    tonalElevation = 0.dp
                ) {
                    tabOrder.forEach { screen ->
                        val isSelected = currentScreen == screen
                        NavigationBarItem(
                            icon = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    // Tiny dot indicator above icon
                                    val dotAlpha by animateFloatAsState(
                                        targetValue = if (isSelected) 1f else 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        ),
                                        label = "dot_${screen.route}"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(width = 20.dp, height = 3.dp)
                                            .clip(RoundedCornerShape(9999.dp))
                                            .background(animatedAccent.copy(alpha = dotAlpha))
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = screen.title,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            },
                            label = { Text(screen.title, fontSize = 10.sp) },
                            selected = isSelected,
                            onClick = {
                                if (screen == Screen.Library) {
                                    if (currentScreen != Screen.Library) previousScreen = currentScreen
                                    currentScreen = Screen.Library
                                    showLibrarySheet = true
                                } else {
                                    currentScreen = screen
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = animatedAccent,
                                selectedTextColor = animatedAccent,
                                unselectedIconColor = Text3,
                                unselectedTextColor = Text3,
                                indicatorColor = Color.Transparent
                            )
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
                val slideSpec: androidx.compose.animation.core.FiniteAnimationSpec<IntOffset> =
                    if (reduceMotion) tween(durationMillis = 0)
                    else tween(durationMillis = 280, easing = FastOutSlowInEasing)
                slideInHorizontally(
                    animationSpec = slideSpec,
                    initialOffsetX = { fullWidth -> direction * fullWidth }
                ) togetherWith slideOutHorizontally(
                    animationSpec = slideSpec,
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
    } // end LocalAccentColor provider
}
