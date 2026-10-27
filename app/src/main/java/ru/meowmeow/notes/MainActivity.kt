package ru.meowmeow.notes

import android.os.Bundle
import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.meowmeow.notes.data.AccentColor
import ru.meowmeow.notes.data.AppSettings
import ru.meowmeow.notes.data.ThemeMode
import ru.meowmeow.notes.ui.OrderDetailScreen
import ru.meowmeow.notes.ui.OrdersScreen
import ru.meowmeow.notes.ui.theme.MeowTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as MeowApp
        setContent {
            val settings by app.settings.settings.collectAsStateWithLifecycle()
            val dark = when (settings.mode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            MeowTheme(settings) {
                SystemBars(dark = dark)
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    MeowRoot(
                        app = app,
                        settings = settings,
                        onThemeMode = app.settings::setMode,
                        onAccent = app.settings::setAccent,
                    )
                }
            }
        }
    }
}

/**
 * Системные панели следуют теме приложения, а не только системной настройке.
 * На Android 15 цвет может игнорироваться, но иконки и фон остаются контрастными.
 */
@Composable
private fun SystemBars(dark: Boolean) {
    val view = LocalView.current
    val background = MaterialTheme.colorScheme.background
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        window.statusBarColor = background.toArgb()
        window.navigationBarColor = background.toArgb()
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
}

/**
 * Навигация на одном состоянии: экран либо список, либо открытая карточка.
 * Так нет лишних зависимостей, а системная «назад» ведёт себя предсказуемо.
 */
@Composable
private fun MeowRoot(
    app: MeowApp,
    settings: AppSettings,
    onThemeMode: (ThemeMode) -> Unit,
    onAccent: (AccentColor) -> Unit,
) {
    var openOrderId by rememberSaveable { mutableStateOf<Long?>(null) }

    BackHandler(enabled = openOrderId != null) {
        openOrderId = null
    }

    val currentId = openOrderId
    if (currentId == null) {
        OrdersScreen(
            repository = app.repository,
            onOpenOrder = { id -> openOrderId = id },
            settings = settings,
            onThemeMode = onThemeMode,
            onAccent = onAccent,
        )
    } else {
        OrderDetailScreen(
            repository = app.repository,
            saveScope = app.appScope,
            orderId = currentId,
            onBack = { openOrderId = null },
            onDeleted = { openOrderId = null },
        )
    }
}
