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
import ru.meowmeow.notes.domain.Section
import ru.meowmeow.notes.domain.StatusDef

data class OrdersUiState(
    val section: Section,
    val statuses: List<StatusDef>,
    val cards: List<OrderCard> = emptyList(),
    val visible: List<OrderCard> = emptyList(),
    val query: String = "",
    val filter: StatusDef? = null,
    val loaded: Boolean = false,
) {
    val activeCount: Int get() = cards.count { section.statusOf(it.order.status).active }
    val doneCount: Int get() = cards.count { section.statusOf(it.order.status).finished }
    val refusedCount: Int get() = cards.count {
        it.order.status == "refused" || it.order.status == "archive"
    }
    val hasFilters: Boolean get() = query.isNotBlank() || filter != null
}

class OrdersViewModel(
    private val repository: OrderRepository,
    private val section: Section,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow<StatusDef?>(null)

    val state: StateFlow<OrdersUiState> =
        combine(repository.cards(section.key), query, filter) { cards, currentQuery, currentFilter ->
            val needle = currentQuery.trim()
            val visible = cards.filter { card ->
                val byText = needle.isEmpty() ||
                    card.order.model.contains(needle, ignoreCase = true) ||
                    card.order.clientName.contains(needle, ignoreCase = true) ||
                    card.order.problem.contains(needle, ignoreCase = true) ||
                    card.order.notes.contains(needle, ignoreCase = true)
                val byStatus = currentFilter == null ||
                    section.statusOf(card.order.status) == currentFilter
                byText && byStatus
            }
            OrdersUiState(
                section = section,
                statuses = section.statuses,
                cards = cards,
                visible = visible,
                query = currentQuery,
                filter = currentFilter,
                loaded = true,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = OrdersUiState(section = section, statuses = section.statuses),
        )

    fun setQuery(value: String) {
        query.value = value
    }

    fun setFilter(value: StatusDef?) {
        filter.value = value
    }

    fun resetFilters() {
        query.value = ""
        filter.value = null
    }

    /** Карточка создаётся сразу в базе: она открывается уже с id. */
    fun createOrder(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            onCreated(repository.createOrder(section.key))
        }
    }

    /** Снимок базы для файла: вызывается из экрана, пока открыт диалог сохранения. */
    suspend fun exportText(): String = repository.exportJson()

    /** Импорт из текста файла: ошибки разбора и записи ловит экран и показывает мастеру. */
    suspend fun importText(text: String): ImportReport = repository.importJson(text)

    companion object {
        fun factory(repository: OrderRepository, section: Section) = viewModelFactory {
            initializer { OrdersViewModel(repository, section) }
        }
    }
}
