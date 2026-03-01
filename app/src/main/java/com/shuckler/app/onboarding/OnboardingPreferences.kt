package com.shuckler.app.onboarding

import android.content.Context
import android.content.SharedPreferences

object OnboardingPreferences {
    private const val PREFS_NAME = "onboarding"
    private const val KEY_HAS_COMPLETED = "has_completed_onboarding"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasCompletedOnboarding(context: Context): Boolean =
        prefs(context).getBoolean(KEY_HAS_COMPLETED, false)

    fun setOnboardingCompleted(context: Context) {
        prefs(context).edit().putBoolean(KEY_HAS_COMPLETED, true).apply()
    }
}
