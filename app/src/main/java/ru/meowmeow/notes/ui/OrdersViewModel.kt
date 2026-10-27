package ru.meowmeow.notes.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.meowmeow.notes.data.OrderCard
import ru.meowmeow.notes.data.ImportReport
import ru.meowmeow.notes.data.OrderRepository
import ru.meowmeow.notes.domain.OrderStatus

data class OrdersUiState(
    val cards: List<OrderCard> = emptyList(),
    val visible: List<OrderCard> = emptyList(),
    val query: String = "",
    val filter: OrderStatus? = null,
    val loaded: Boolean = false,
) {
    val activeCount: Int get() = cards.count { it.order.status.isActive }
    val doneCount: Int get() = cards.count { it.order.status == OrderStatus.DONE }
    val refusedCount: Int get() = cards.count { it.order.status == OrderStatus.REFUSED }
    val hasFilters: Boolean get() = query.isNotBlank() || filter != null
}

class OrdersViewModel(private val repository: OrderRepository) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow<OrderStatus?>(null)

    val state: StateFlow<OrdersUiState> =
        combine(repository.cards, query, filter) { cards, currentQuery, currentFilter ->
            val needle = currentQuery.trim()
            val visible = cards.filter { card ->
                val byText = needle.isEmpty() ||
                    card.order.model.contains(needle, ignoreCase = true) ||
                    card.order.clientName.contains(needle, ignoreCase = true) ||
                    card.order.problem.contains(needle, ignoreCase = true)
                val byStatus = currentFilter == null || card.order.status == currentFilter
                byText && byStatus
            }
            OrdersUiState(
                cards = cards,
                visible = visible,
                query = currentQuery,
                filter = currentFilter,
                loaded = true,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = OrdersUiState(),
        )

    fun setQuery(value: String) {
        query.value = value
    }

    fun setFilter(value: OrderStatus?) {
        filter.value = value
    }

    fun resetFilters() {
        query.value = ""
        filter.value = null
    }

    /** Заказ создаётся сразу в базе: карточка открывается уже с id. */
    fun createOrder(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            onCreated(repository.createOrder())
        }
    }

    /** Снимок базы для файла: вызывается из экрана, пока открыт диалог сохранения. */
    suspend fun exportText(): String = repository.exportJson()

    /** Импорт из текста файла: ошибки разбора и записи ловит экран и показывает мастеру. */
    suspend fun importText(text: String): ImportReport = repository.importJson(text)

    companion object {
        fun factory(repository: OrderRepository) = viewModelFactory {
            initializer { OrdersViewModel(repository) }
        }
    }
}
