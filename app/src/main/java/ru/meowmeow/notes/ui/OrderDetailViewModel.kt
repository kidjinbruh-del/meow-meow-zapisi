package ru.meowmeow.notes.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.meowmeow.notes.data.ChecklistItemEntity
import ru.meowmeow.notes.data.OrderEntity
import ru.meowmeow.notes.data.OrderRepository
import ru.meowmeow.notes.data.PartEntity
import ru.meowmeow.notes.data.PhotoEntity
import ru.meowmeow.notes.domain.OrderStatus
import java.io.File

/**
 * Карточка заказа. Текстовые поля живут в черновике и сохраняются с задержкой,
 * чек-лист/запчасти/фото — сразу, потому что это отдельные действия.
 */
class OrderDetailViewModel(
    private val repository: OrderRepository,
    private val orderId: Long,
    private val saveScope: CoroutineScope,
) : ViewModel() {

    private val _draft = MutableStateFlow<OrderEntity?>(null)
    val draft: StateFlow<OrderEntity?> = _draft.asStateFlow()

    val checklist: StateFlow<List<ChecklistItemEntity>?> =
        repository.observeChecklist(orderId)
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val parts: StateFlow<List<PartEntity>?> =
        repository.observeParts(orderId)
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val photos: StateFlow<List<PhotoEntity>?> =
        repository.observePhotos(orderId)
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private var saveJob: Job? = null

    init {
        viewModelScope.launch {
            if (_draft.value == null) {
                _draft.value = repository.getOrder(orderId)
            }
        }
    }

    fun edit(transform: (OrderEntity) -> OrderEntity) {
        val current = _draft.value ?: return
        _draft.value = transform(current)
        saveJob?.cancel()
        saveJob = saveScope.launch {
            delay(SAVE_DELAY_MS)
            persist()
        }
    }

    fun setStatus(status: OrderStatus) {
        edit { it.copy(status = status) }
        flush()
    }

    /** Сохранить немедленно: вызывается при выходе и сворачивании. */
    fun flush() {
        saveJob?.cancel()
        saveScope.launch { persist() }
    }

    private suspend fun persist() {
        val current = _draft.value ?: return
        repository.save(current)
    }

    fun addChecklist(label: String) {
        viewModelScope.launch { repository.addChecklistItem(orderId, label) }
    }

    fun toggleChecklist(item: ChecklistItemEntity, checked: Boolean) {
        viewModelScope.launch { repository.setChecklistChecked(item, checked) }
    }

    fun removeChecklist(item: ChecklistItemEntity) {
        viewModelScope.launch { repository.deleteChecklistItem(item) }
    }

    fun addPart(name: String) {
        viewModelScope.launch { repository.addPart(orderId, name) }
    }

    fun setPartBought(part: PartEntity, bought: Boolean) {
        viewModelScope.launch { repository.setPartBought(part, bought) }
    }

    fun changeQuantity(part: PartEntity, delta: Int) {
        viewModelScope.launch { repository.setPartQuantity(part, part.quantity + delta) }
    }

    fun renamePart(part: PartEntity, name: String) {
        viewModelScope.launch { repository.renamePart(part, name) }
    }

    fun removePart(part: PartEntity) {
        viewModelScope.launch { repository.deletePart(part) }
    }

    fun attachPhoto(file: File) {
        viewModelScope.launch { repository.attachPhoto(orderId, file) }
    }

    fun importPhoto(uri: Uri) {
        viewModelScope.launch { repository.importPhoto(orderId, uri) }
    }

    fun removePhoto(photo: PhotoEntity) {
        viewModelScope.launch { repository.deletePhoto(photo) }
    }

    fun setPhotosPinned(photos: List<PhotoEntity>, pinned: Boolean) {
        viewModelScope.launch { repository.setPhotosPinned(photos, pinned) }
    }

    /** Файл создаётся заранее: камера должна писать уже в готовый путь. */
    suspend fun newPhotoTarget(): File = withContext(Dispatchers.IO) {
        repository.newPhotoFile(orderId)
    }

    fun deleteOrder(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteOrder(orderId)
            onDeleted()
        }
    }

    companion object {
        private const val SAVE_DELAY_MS = 250L

        fun factory(repository: OrderRepository, orderId: Long, saveScope: CoroutineScope) =
            viewModelFactory {
                initializer { OrderDetailViewModel(repository, orderId, saveScope) }
            }
    }
}
