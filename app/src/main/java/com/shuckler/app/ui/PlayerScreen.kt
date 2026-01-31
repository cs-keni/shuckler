package com.shuckler.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shuckler.app.player.PlayerViewModel

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = viewModel(
        factory = PlayerViewModel.Factory(LocalContext.current)
    )
) {
    val isPlaying by viewModel.isPlaying.collectAsState()
    val trackTitle by viewModel.currentTrackTitle.collectAsState()
    val trackArtist by viewModel.currentTrackArtist.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = trackTitle,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = trackArtist,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { /* Previous - Phase 4 (no queue yet) */ }
            ) {
                Text("Previous")
            }
            Button(
                onClick = { viewModel.togglePlayPause() }
            ) {
                Text(if (isPlaying) "Pause" else "Play")
            }
            Button(
                onClick = { /* Next - Phase 4 (no queue yet) */ }
            ) {
                Text("Next")
            }
        }

        Button(
            onClick = { /* Loop toggle - Phase 9 */ },
            modifier = Modifier.padding(top = 32.dp)
        ) {
            Text("Loop: Off")
        }
    }
}
