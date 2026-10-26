package ru.meowmeow.notes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import ru.meowmeow.notes.ui.OrderDetailScreen
import ru.meowmeow.notes.ui.OrdersScreen
import ru.meowmeow.notes.ui.theme.MeowTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as MeowApp
        setContent {
            MeowTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    MeowRoot(app)
                }
            }
        }
    }
}

/**
 * Навигация на одном состоянии: экран либо список, либо открытая карточка.
 * Так нет лишних зависимостей, а системная «назад» ведёт себя предсказуемо.
 */
@Composable
private fun MeowRoot(app: MeowApp) {
    var openOrderId by rememberSaveable { mutableStateOf<Long?>(null) }

    BackHandler(enabled = openOrderId != null) {
        openOrderId = null
    }

    val currentId = openOrderId
    if (currentId == null) {
        OrdersScreen(
            repository = app.repository,
            onOpenOrder = { id -> openOrderId = id },
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
