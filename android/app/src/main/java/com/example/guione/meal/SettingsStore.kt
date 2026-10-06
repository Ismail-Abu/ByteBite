package com.example.guione.meal

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Small persisted app preferences (SharedPreferences). The theme choice is held
 * as Compose state so the app recomposes into the new theme immediately and the
 * value survives restarts.
 */
class SettingsStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("bytebite.settings", Context.MODE_PRIVATE)

    var themeMode: ThemeMode by mutableStateOf(
        runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name)!!) }
            .getOrDefault(ThemeMode.SYSTEM)
    )
        private set

    fun setTheme(mode: ThemeMode) {
        themeMode = mode
        prefs.edit().putString(KEY_THEME, mode.name).apply()
    }

    private companion object {
        const val KEY_THEME = "theme_mode"
    }
}
