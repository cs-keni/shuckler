package com.shuckler.app.util

import android.content.Context
import android.content.Intent

/**
 * Share text via system share sheet.
 * Phase 46: Share track & playlist.
 */
fun shareText(context: Context, text: String, title: String? = "Share") {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, title))
}
