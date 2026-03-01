package com.shuckler.app.achievement

import androidx.compose.runtime.compositionLocalOf

val LocalAchievementManager = compositionLocalOf<AchievementManager> {
    error("No AchievementManager provided")
}
