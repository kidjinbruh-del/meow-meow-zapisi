package ru.meowmeow.notes.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.meowmeow.notes.data.ChecklistItemEntity
import ru.meowmeow.notes.data.OrderRepository
import ru.meowmeow.notes.data.PartEntity
import ru.meowmeow.notes.data.PhotoEntity
import ru.meowmeow.notes.data.PhotoStore
import ru.meowmeow.notes.domain.Defaults
import ru.meowmeow.notes.domain.OrderStatus
import ru.meowmeow.notes.domain.checkedLabel
import ru.meowmeow.notes.domain.formatWhen
import ru.meowmeow.notes.domain.photosLabel
import ru.meowmeow.notes.ui.theme.Meow
import ru.meowmeow.notes.ui.theme.soft
import ru.meowmeow.notes.ui.theme.tint
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OrderDetailScreen(
    repository: OrderRepository,
    saveScope: kotlinx.coroutines.CoroutineScope,
    orderId: Long,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
) {
    val viewModel: OrderDetailViewModel = viewModel(
        key = "order-$orderId",
        factory = OrderDetailViewModel.factory(repository, orderId, saveScope),
    )
    val order by viewModel.draft.collectAsStateWithLifecycle()
    val checklistState by viewModel.checklist.collectAsStateWithLifecycle()
    val partsState by viewModel.parts.collectAsStateWithLifecycle()
    val photosState by viewModel.photos.collectAsStateWithLifecycle()
    val checklist = checklistState.orEmpty()
    val parts = partsState.orEmpty()
    val photos = photosState.orEmpty()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingFile by remember { mutableStateOf<File?>(null) }
    var askDelete by rememberSaveable { mutableStateOf(false) }
    var viewer by remember { mutableStateOf<PhotoEntity?>(null) }
    var renaming by remember { mutableStateOf<PartEntity?>(null) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val file = pendingFile
        pendingFile = null
        // Ненулевой файл без успеха = камера закрылась, мусор убираем.
        if (success && file != null) viewModel.attachPhoto(file) else file?.delete()
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.importPhoto(uri)
    }

    // Правки сохраняются при уходе с экрана и при сворачивании приложения.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.flush()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.flush()
        }
    }

    val current = order

    fun shoot() {
        scope.launch {
            val file = viewModel.newPhotoTarget()
            pendingFile = file
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            runCatching { camera.launch(uri) }.onFailure {
                file.delete()
                pendingFile = null
                Toast.makeText(context, "Камера недоступна", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = current?.model?.ifBlank { "Новый заказ" } ?: "Заказ",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (current != null) {
                            Text(
                                text = "создан ${formatWhen(current.createdAt)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.flush()
                            onBack()
                        },
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { askDelete = true }) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Удалить заказ",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { innerPadding ->
        if (current == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 28.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    StatusSection(status = current.status, onSelect = viewModel::setStatus)
                }

                item {
                    SectionCard(title = "Техника и клиент", subtitle = "обязательное") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Field(
                                value = current.model,
                                onValueChange = { value -> viewModel.edit { it.copy(model = value) } },
                                label = "Модель техники",
                                placeholder = "LG WD-10480",
                            )
                            Field(
                                value = current.clientName,
                                onValueChange = { value -> viewModel.edit { it.copy(clientName = value) } },
                                label = "Имя клиента",
                                placeholder = "Анна",
                            )
                            Field(
                                value = current.clientContact,
                                onValueChange = { value -> viewModel.edit { it.copy(clientContact = value) } },
                                label = "Контакт",
                                placeholder = "+7 900 000-00-00",
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone,
                            )
                            Field(
                                value = current.problem,
                                onValueChange = { value -> viewModel.edit { it.copy(problem = value) } },
                                label = "Проблема со слов клиента",
                                placeholder = "Не отжимает, гудит при отжиме",
                                singleLine = false,
                                minLines = 3,
                            )
                        }
                    }
                }

                item {
                    ChecklistSection(
                        items = checklist,
                        loaded = checklistState != null,
                        onToggle = viewModel::toggleChecklist,
                        onDelete = viewModel::removeChecklist,
                        onAdd = viewModel::addChecklist,
                    )
                }

                item {
                    PartsSection(
                        items = parts,
                        loaded = partsState != null,
                        onToggle = viewModel::setPartBought,
                        onMinus = { part -> viewModel.changeQuantity(part, -1) },
                        onPlus = { part -> viewModel.changeQuantity(part, 1) },
                        onRename = { part -> renaming = part },
                        onDelete = viewModel::removePart,
                        onAdd = { name -> viewModel.addPart(name) },
                    )
                }

                item {
                    SectionCard(title = "Заметки", subtitle = "сохраняется само") {
                        Field(
                            value = current.notes,
                            onValueChange = { value -> viewModel.edit { it.copy(notes = value) } },
                            label = "Ход ремонта",
                            placeholder = "Купил аналог, не подошел…",
                            singleLine = false,
                            minLines = 4,
                        )
                    }
                }

                item {
                    PhotosSection(
                        photos = photos,
                        loaded = photosState != null,
                        onOpen = { photo -> viewer = photo },
                        onCamera = { shoot() },
                        onGallery = {
                            gallery.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                    )
                }

                item {
                    Text(
                        text = "Заказ №${current.id}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                    )
                }
            }
        }
    }

    if (askDelete) {
        AlertDialog(
            onDismissRequest = { askDelete = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Удалить заказ?") },
            text = {
                Text(
                    "Пропадут карточка, чек-лист, запчасти и фото (${photos.size} шт.). " +
                        "Восстановить не получится.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        askDelete = false
                        viewModel.deleteOrder(onDeleted)
                    },
                ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { askDelete = false }) { Text("Отмена") }
            },
        )
    }

    val photo = viewer
    if (photo != null) {
        PhotoViewer(
            photo = photo,
            onDelete = {
                viewModel.removePhoto(photo)
                viewer = null
            },
            onDismiss = { viewer = null },
        )
    }

    val part = renaming
    if (part != null) {
        var name by remember(part.id) { mutableStateOf(part.name) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Название детали") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isNotBlank()) viewModel.renamePart(part, name)
                        renaming = null
                    },
                ) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { renaming = null }) { Text("Отмена") }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusSection(status: OrderStatus, onSelect: (OrderStatus) -> Unit) {
    SectionCard(title = "Статус ремонта", subtitle = status.label) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OrderStatus.entries.forEach { option ->
                StatusOption(
                    status = option,
                    selected = option == status,
                    onClick = { onSelect(option) },
                )
            }
        }
    }
}

@Composable
private fun StatusOption(status: OrderStatus, selected: Boolean, onClick: () -> Unit) {
    val foreground = if (selected) Color.White else status.tint()
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) status.tint() else status.soft())
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (!selected) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(status.tint()),
            )
        }
        Text(
            text = status.short,
            style = MaterialTheme.typography.labelLarge,
            color = foreground,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChecklistSection(
    items: List<ChecklistItemEntity>,
    loaded: Boolean,
    onToggle: (ChecklistItemEntity, Boolean) -> Unit,
    onDelete: (ChecklistItemEntity) -> Unit,
    onAdd: (String) -> Unit,
) {
    SectionCard(
        title = "Что проверено",
        subtitle = when {
            !loaded -> null
            items.isEmpty() -> "пусто"
            else -> checkedLabel(items.count { it.isChecked }, items.size)
        },
    ) {
        if (!loaded) {
            SectionLoading("Загружаем проверки…")
            return@SectionCard
        }
        items.forEach { item ->
            key(item.id) {
                ChecklistRow(item = item, onToggle = onToggle, onDelete = onDelete)
            }
        }
        if (items.isEmpty()) {
            Text(
                text = "Отмечайте проверки по мере диагностики",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        val existing = items.map { it.label.lowercase() }.toSet()
        val tips = Defaults.suggestions.filter { it.lowercase() !in existing }
        if (tips.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                tips.forEach { tip ->
                    AssistChip(
                        onClick = { onAdd(tip) },
                        label = { Text("+ $tip", style = MaterialTheme.typography.labelMedium) },
                        shape = RoundedCornerShape(50),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color.Transparent,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        AddInline(placeholder = "Своя проверка", onAdd = onAdd, buttonDescription = "Добавить проверку")
    }
}

@Composable
private fun ChecklistRow(
    item: ChecklistItemEntity,
    onToggle: (ChecklistItemEntity, Boolean) -> Unit,
    onDelete: (ChecklistItemEntity) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onToggle(item, !item.isChecked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = item.isChecked,
            onCheckedChange = { checked -> onToggle(item, checked) },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.outline,
                checkmarkColor = MaterialTheme.colorScheme.onPrimary,
            ),
        )
        Text(
            text = item.label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (item.isChecked) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { onDelete(item) }, modifier = Modifier.size(34.dp)) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Удалить проверку",
                modifier = Modifier.size(17.dp),
                tint = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun PartsSection(
    items: List<PartEntity>,
    loaded: Boolean,
    onToggle: (PartEntity, Boolean) -> Unit,
    onMinus: (PartEntity) -> Unit,
    onPlus: (PartEntity) -> Unit,
    onRename: (PartEntity) -> Unit,
    onDelete: (PartEntity) -> Unit,
    onAdd: (String) -> Unit,
) {
    val bought = items.count { it.isBought }
    SectionCard(
        title = "Запчасти",
        subtitle = when {
            !loaded -> null
            items.isEmpty() -> "пока не нужны"
            else -> "$bought из ${items.size} куплено"
        },
    ) {
        if (!loaded) {
            SectionLoading("Загружаем запчасти…")
            return@SectionCard
        }
        items.forEach { part ->
            key(part.id) {
                PartRow(
                    part = part,
                    onToggle = onToggle,
                    onMinus = onMinus,
                    onPlus = onPlus,
                    onRename = onRename,
                    onDelete = onDelete,
                )
            }
        }
        if (items.isEmpty()) {
            Text(
                text = "Добавьте деталь, которую надо купить",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(10.dp))
        AddInline(placeholder = "Название детали", onAdd = onAdd, buttonDescription = "Добавить деталь")
    }
}

@Composable
private fun PartRow(
    part: PartEntity,
    onToggle: (PartEntity, Boolean) -> Unit,
    onMinus: (PartEntity) -> Unit,
    onPlus: (PartEntity) -> Unit,
    onRename: (PartEntity) -> Unit,
    onDelete: (PartEntity) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Checkbox(
            checked = part.isBought,
            onCheckedChange = { checked -> onToggle(part, checked) },
            colors = CheckboxDefaults.colors(
                checkedColor = Meow.Ready,
                uncheckedColor = MaterialTheme.colorScheme.outline,
                checkmarkColor = Color.White,
            ),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 2.dp),
        ) {
            Text(
                text = part.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (part.isBought) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                textDecoration = if (part.isBought) TextDecoration.LineThrough else TextDecoration.None,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onRename(part) }
                    .padding(vertical = 2.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (part.isBought) "Куплено" else "Не куплено",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (part.isBought) Meow.Ready else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onMinus(part) },
                        enabled = part.quantity > 1,
                        modifier = Modifier.size(30.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Remove,
                            contentDescription = "Меньше",
                            modifier = Modifier.size(15.dp),
                        )
                    }
                    Text(
                        text = part.quantity.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(min = 20.dp),
                    )
                    IconButton(onClick = { onPlus(part) }, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Больше",
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
                IconButton(onClick = { onDelete(part) }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Удалить запчасть",
                        modifier = Modifier.size(17.dp),
                        tint = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotosSection(
    photos: List<PhotoEntity>,
    loaded: Boolean,
    onOpen: (PhotoEntity) -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
) {
    SectionCard(
        title = "Фото",
        subtitle = if (photos.isEmpty()) "до/после разборки" else photosLabel(photos.size),
    ) {
        if (!loaded) {
            SectionLoading("Загружаем фото…")
            return@SectionCard
        }
        if (photos.isNotEmpty()) {
            photos.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    row.forEach { photo ->
                        PhotoCell(
                            photo = photo,
                            modifier = Modifier.weight(1f),
                            onClick = { onOpen(photo) },
                        )
                    }
                    repeat(3 - row.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onCamera,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Снять", style = MaterialTheme.typography.labelLarge)
            }
            OutlinedButton(
                onClick = onGallery,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Галерея", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun PhotoCell(photo: PhotoEntity, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, photo.fileName) {
        value = withContext(Dispatchers.IO) {
            PhotoStore.thumbnail(context.applicationContext, photo.fileName, 320)?.asImageBitmap()
        }
    }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val image = bitmap
        if (image != null) {
            androidx.compose.foundation.Image(
                bitmap = image,
                contentDescription = "Фото заказа",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.BrokenImage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun PhotoViewer(photo: PhotoEntity, onDelete: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, photo.fileName) {
        value = withContext(Dispatchers.IO) {
            PhotoStore.full(context.applicationContext, photo.fileName)?.asImageBitmap()
        }
    }
    var askDelete by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp),
                contentAlignment = Alignment.Center,
            ) {
                val image = bitmap
                if (image != null) {
                    androidx.compose.foundation.Image(
                        bitmap = image,
                        contentDescription = "Фото заказа",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(32.dp),
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatWhen(photo.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { askDelete = true }) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) { Text("Закрыть") }
            }
        }
    }

    if (askDelete) {
        AlertDialog(
            onDismissRequest = { askDelete = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Удалить фото?") },
            text = { Text("Файл будет удалён с телефона.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        askDelete = false
                        onDelete()
                    },
                ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { askDelete = false }) { Text("Отмена") }
            },
        )
    }
}
