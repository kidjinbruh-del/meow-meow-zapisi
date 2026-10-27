package ru.meowmeow.notes.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Тема оформления: как в системе или принудительно светлая/тёмная. */
enum class ThemeMode(val key: String, val label: String) {
    SYSTEM("system", "Системная"),
    LIGHT("light", "Светлая"),
    DARK("dark", "Тёмная");

    companion object {
        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

/** Акцентный цвет кнопок и активных элементов. */
enum class AccentColor(val key: String, val label: String) {
    GREEN("green", "Зелёный"),
    AMBER("amber", "Янтарный"),
    BLUE("blue", "Синий");

    companion object {
        fun fromKey(key: String?): AccentColor = entries.firstOrNull { it.key == key } ?: GREEN
    }
}

data class AppSettings(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val accent: AccentColor = AccentColor.GREEN,
)

/**
 * Настройки оформления лежат в SharedPreferences и отдаются потоком:
 * экран перерисовывается сразу после выбора, без перезапуска.
 */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("meow-settings", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun setMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_MODE, mode.key).apply()
        _settings.update { it.copy(mode = mode) }
    }

    fun setAccent(accent: AccentColor) {
        prefs.edit().putString(KEY_ACCENT, accent.key).apply()
        _settings.update { it.copy(accent = accent) }
    }

    private fun read(): AppSettings = AppSettings(
        mode = ThemeMode.fromKey(prefs.getString(KEY_MODE, null)),
        accent = AccentColor.fromKey(prefs.getString(KEY_ACCENT, null)),
    )

    private companion object {
        const val KEY_MODE = "theme_mode"
        const val KEY_ACCENT = "accent"
    }
}
