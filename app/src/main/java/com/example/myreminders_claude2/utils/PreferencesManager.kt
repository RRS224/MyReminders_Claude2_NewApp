package com.example.myreminders_claude2.utils

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "my_reminders_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_PERMISSIONS_SKIPPED = "permissions_skipped"
        private const val KEY_BATTERY_CARD_DISMISSED = "battery_card_dismissed"
    }

    var hasCompletedOnboarding: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    var hasSkippedPermissions: Boolean
        get() = prefs.getBoolean(KEY_PERMISSIONS_SKIPPED, false)
        set(value) = prefs.edit().putBoolean(KEY_PERMISSIONS_SKIPPED, value).apply()

    var hasDismissedBatteryCard: Boolean
        get() = prefs.getBoolean(KEY_BATTERY_CARD_DISMISSED, false)
        set(value) = prefs.edit().putBoolean(KEY_BATTERY_CARD_DISMISSED, value).apply()

    fun resetOnboarding() {
        prefs.edit()
            .putBoolean(KEY_ONBOARDING_COMPLETED, false)
            .putBoolean(KEY_PERMISSIONS_SKIPPED, false)
            .apply()
    }
}