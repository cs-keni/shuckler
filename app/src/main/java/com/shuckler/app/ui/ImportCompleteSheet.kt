package com.shuckler.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.shuckler.app.ShucklerApplication
import com.shuckler.app.spotify.ImportState
import com.shuckler.app.ui.theme.Base
import com.shuckler.app.ui.theme.LocalAccentColor
import com.shuckler.app.ui.theme.Surface
import com.shuckler.app.ui.theme.SurfaceElevated
import com.shuckler.app.ui.theme.Text1
import com.shuckler.app.ui.theme.Text2
import com.shuckler.app.ui.theme.Text3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportCompleteSheet(
    importId: String,
    onDismiss: () -> Unit,
    onReviewMismatches: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as? ShucklerApplication
    val records = app?.spotifyImportManager?.getImportRecords(importId) ?: emptyList()
    val matched = records.count { it.state == ImportState.COMPLETED }
    val notFound = records.count { it.state == ImportState.NOT_FOUND }
    val total = records.size

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface,
        contentColor = Text1,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "$matched ${if (matched == 1) "song" else "songs"} rescued",
                style = MaterialTheme.typography.displaySmall,
                color = Text1
            )
            Text(
                text = "out of $total total",
                style = MaterialTheme.typography.bodyMedium,
                color = Text2
            )

            if (notFound > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceElevated, RoundedCornerShape(10.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "$notFound tracks couldn't be found",
                            style = MaterialTheme.typography.titleSmall,
                            color = Text1
                        )
                        Text(
                            "Match them manually before cancelling",
                            style = MaterialTheme.typography.bodySmall,
                            color = Text2
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = { onDismiss(); onReviewMismatches() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = LocalAccentColor.current,
                        contentColor = Base
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Review unmatched tracks")
                }
            } else {
                // 100% match — show Cancel Spotify CTA directly
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Your library is complete. Ready to save $13/month?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Text2
                )
                Spacer(modifier = Modifier.height(4.dp))
                CancelSpotifyButton(onDone = onDismiss)
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun CancelSpotifyButton(onDone: () -> Unit = {}) {
    val context = LocalContext.current
    val app = context.applicationContext as? ShucklerApplication

    Button(
        onClick = {
            app?.spotifySavingsTracker?.setCancelledToday()
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.spotify.com/account/subscription/"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            onDone()
        },
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = LocalAccentColor.current,
            contentColor = Base
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text("Cancel Spotify · Save \$13/month")
        Spacer(modifier = Modifier.size(6.dp))
        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
    }
}
