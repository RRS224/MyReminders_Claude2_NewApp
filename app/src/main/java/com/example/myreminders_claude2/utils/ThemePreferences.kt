package com.example.myreminders_claude2.utils

import android.content.Context
import android.content.SharedPreferences

class ThemePreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_USE_AMOLED = "use_amoled"
        private const val KEY_USE_DYNAMIC_COLORS = "use_dynamic_colors"
        private const val KEY_AUTO_DARK_ENABLED = "auto_dark_enabled"
        private const val KEY_AUTO_DARK_START_HOUR = "auto_dark_start_hour"
        private const val KEY_AUTO_DARK_END_HOUR = "auto_dark_end_hour"

        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val THEME_SYSTEM = "system"
    }

    // Theme mode: light, dark, or system
    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, THEME_SYSTEM) ?: THEME_SYSTEM
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value).apply()

    // AMOLED Black mode (pure #000000 for dark theme)
    var useAmoledBlack: Boolean
        get() = prefs.getBoolean(KEY_USE_AMOLED, false)
        set(value) = prefs.edit().putBoolean(KEY_USE_AMOLED, value).apply()

    // Dynamic Material You colors (Android 12+)
    var useDynamicColors: Boolean
        get() = prefs.getBoolean(KEY_USE_DYNAMIC_COLORS, true)
        set(value) = prefs.edit().putBoolean(KEY_USE_DYNAMIC_COLORS, value).apply()

    // Auto dark mode scheduling
    var autoDarkEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_DARK_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_DARK_ENABLED, value).apply()

    // Auto dark mode start time (default: 8 PM = 20)
    var autoDarkStartHour: Int
        get() = prefs.getInt(KEY_AUTO_DARK_START_HOUR, 20)
        set(value) = prefs.edit().putInt(KEY_AUTO_DARK_START_HOUR, value).apply()

    // Auto dark mode end time (default: 7 AM = 7)
    var autoDarkEndHour: Int
        get() = prefs.getInt(KEY_AUTO_DARK_END_HOUR, 7)
        set(value) = prefs.edit().putInt(KEY_AUTO_DARK_END_HOUR, value).apply()

    /**
     * Check if dark mode should be active based on auto-schedule
     */
    fun shouldUseDarkMode(): Boolean {
        if (!autoDarkEnabled) {
            return when (themeMode) {
                THEME_LIGHT -> false
                THEME_DARK -> true
                else -> false // Will use system default
            }
        }

        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val startHour = autoDarkStartHour
        val endHour = autoDarkEndHour

        return if (startHour < endHour) {
            // Normal case: e.g., 8 AM to 8 PM
            currentHour in startHour until endHour
        } else {
            // Overnight case: e.g., 8 PM to 7 AM
            currentHour >= startHour || currentHour < endHour
        }
    }
}