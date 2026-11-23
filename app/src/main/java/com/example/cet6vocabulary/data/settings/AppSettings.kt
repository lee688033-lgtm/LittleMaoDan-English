package com.example.cet6vocabulary.data.settings

import android.content.Context

enum class ThemeMode(val label: String) {
    SYSTEM("跟随系统"),
    LIGHT("浅色"),
    DARK("深色")
}

/** Small key-value store for preferences that must outlive the process but are not user data. */
class AppSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var themeMode: ThemeMode
        get() = prefs.getString(KEY_THEME_MODE, null)
            ?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
            ?: ThemeMode.SYSTEM
        set(value) {
            prefs.edit().putString(KEY_THEME_MODE, value.name).apply()
        }

    private companion object {
        const val PREFS_NAME = "cet6_settings"
        const val KEY_THEME_MODE = "theme_mode"
    }
}
