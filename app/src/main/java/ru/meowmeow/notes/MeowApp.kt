package ru.meowmeow.notes

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import ru.meowmeow.notes.data.AppDatabase
import ru.meowmeow.notes.data.OrderRepository

class MeowApp : Application() {

    lateinit var repository: OrderRepository
        private set

    /**
     * Долгоживущий scope для автосохранения: правки не должны теряться при
     * повороте экрана или выходе с карточки заказа.
     */
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.create(this)
        repository = OrderRepository(this, database.orderDao())
    }
}
