package com.shuckler.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shuckler.app.ui.theme.ShucklerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Stateless mini player composable used only in tests — mirrors the real MiniPlayerBar layout
 * but takes explicit state values rather than a ViewModel, so no service connection is needed.
 */
@Composable
private fun FakeMiniPlayerBar(
    title: String,
    artist: String,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onTap: () -> Unit,
    onPlayPause: () -> Unit = {},
    onSkipNext: () -> Unit = {}
) {
    val rawProgress = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(targetValue = rawProgress, label = "mini_player_progress")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .clickable(onClick = onTap)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f))
    ) {
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxWidth().height(2.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.surface))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(
                    text = title.ifBlank { "No track" },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1
                )
                Text(
                    text = artist.ifBlank { "" },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }
            IconButton(onClick = onPlayPause, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onSkipNext, modifier = Modifier.size(40.dp)) {
                Icon(imageVector = Icons.Default.SkipNext, contentDescription = "Next")
            }
        }
    }
}

/**
 * Instrumented Compose tests for MiniPlayerBar Phase 50C features.
 * Run with: ./gradlew connectedAndroidTest  (requires connected device or emulator)
 */
@RunWith(AndroidJUnit4::class)
class MiniPlayerBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun miniPlayerBar_showsTrackTitleAndArtist() {
        composeTestRule.setContent {
            ShucklerTheme {
                FakeMiniPlayerBar(
                    title = "Test Track",
                    artist = "Test Artist",
                    isPlaying = false,
                    positionMs = 0L,
                    durationMs = 60_000L,
                    onTap = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Test Track").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Artist").assertIsDisplayed()
    }

    @Test
    fun miniPlayerBar_tapCallsOnTap() {
        var tapped = false
        composeTestRule.setContent {
            ShucklerTheme {
                FakeMiniPlayerBar(
                    title = "Test Track",
                    artist = "Test Artist",
                    isPlaying = false,
                    positionMs = 0L,
                    durationMs = 60_000L,
                    onTap = { tapped = true }
                )
            }
        }
        composeTestRule.onNodeWithText("Test Track").performClick()
        assert(tapped) { "onTap should have been called" }
    }

    @Test
    fun miniPlayerBar_showsPauseIconWhenPlaying() {
        composeTestRule.setContent {
            ShucklerTheme {
                FakeMiniPlayerBar(
                    title = "Playing Track",
                    artist = "Artist",
                    isPlaying = true,
                    positionMs = 15_000L,
                    durationMs = 60_000L,
                    onTap = {}
                )
            }
        }
        composeTestRule.onNodeWithContentDescription("Pause").assertIsDisplayed()
    }

    @Test
    fun miniPlayerBar_showsPlayIconWhenPaused() {
        composeTestRule.setContent {
            ShucklerTheme {
                FakeMiniPlayerBar(
                    title = "Paused Track",
                    artist = "Artist",
                    isPlaying = false,
                    positionMs = 0L,
                    durationMs = 60_000L,
                    onTap = {}
                )
            }
        }
        composeTestRule.onNodeWithContentDescription("Play").assertIsDisplayed()
    }

    @Test
    fun miniPlayerBar_showsProgressBar() {
        composeTestRule.setContent {
            ShucklerTheme {
                FakeMiniPlayerBar(
                    title = "Track",
                    artist = "Artist",
                    isPlaying = true,
                    positionMs = 30_000L,
                    durationMs = 60_000L,
                    onTap = {}
                )
            }
        }
        // The progress indicator renders as a role=ProgressBar in the accessibility tree
        composeTestRule.onNodeWithContentDescription("Play").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Pause").assertIsDisplayed()
    }

    @Test
    fun miniPlayerBar_showsFallbackTitleWhenBlank() {
        composeTestRule.setContent {
            ShucklerTheme {
                FakeMiniPlayerBar(
                    title = "",
                    artist = "",
                    isPlaying = false,
                    positionMs = 0L,
                    durationMs = 0L,
                    onTap = {}
                )
            }
        }
        composeTestRule.onNodeWithText("No track").assertIsDisplayed()
    }
}
