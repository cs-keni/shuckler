package com.shuckler.app.personality

import androidx.compose.runtime.compositionLocalOf

val LocalListeningPersonalityManager = compositionLocalOf<ListeningPersonalityManager> {
    error("No ListeningPersonalityManager provided")
}
