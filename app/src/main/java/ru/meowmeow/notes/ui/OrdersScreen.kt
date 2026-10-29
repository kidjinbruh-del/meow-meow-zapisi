package ru.meowmeow.notes.ui

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.meowmeow.notes.data.AccentColor
import ru.meowmeow.notes.data.AppSettings
import ru.meowmeow.notes.data.ImportReport
import ru.meowmeow.notes.data.OrderCard
import ru.meowmeow.notes.data.OrderRepository
import ru.meowmeow.notes.data.ThemeMode
import ru.meowmeow.notes.domain.Section
import ru.meowmeow.notes.domain.StatusDef
import ru.meowmeow.notes.domain.formatWhen
import ru.meowmeow.notes.domain.partsLabel
import ru.meowmeow.notes.domain.photosLabel
import ru.meowmeow.notes.domain.problemPreview
import ru.meowmeow.notes.ui.theme.tint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Главный экран: вкладки разделов, поиск, фильтр по статусу раздела и список карточек.
 * Кнопка «+» создаёт карточку в текущем разделе и сразу открывает её.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OrdersScreen(
    repository: OrderRepository,
    onOpenOrder: (Long) -> Unit,
    settings: AppSettings = AppSettings(),
    onThemeMode: (ThemeMode) -> Unit = {},
    onAccent: (AccentColor) -> Unit = {},
) {
    var section by rememberSaveable { mutableStateOf(Section.ORDERS) }
    val viewModel: OrdersViewModel = viewModel(
        key = "orders-${section.key}",
        factory = OrdersViewModel.factory(repository, section),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showThemeSheet by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Архив: сначала системный диалог выбора файла, затем одна операция.
    // На Android 10+ доступ к чужому файлу идёт только через SAF (URI).
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val text = viewModel.exportText()
                    withContext(Dispatchers.IO) {
                        val stream = context.contentResolver.openOutputStream(uri)
                            ?: error("не удалось открыть файл")
                        stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                    }
                }.onSuccess {
                    toast(context, "Архив сохранён")
                }.onFailure { error ->
                    toast(context, "Не удалось сохранить: ${error.message ?: "ошибка"}")
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val text = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)
                            ?.use { it.readBytes().toString(Charsets.UTF_8) }
                            ?: error("не удалось прочитать файл")
                    }
                    viewModel.importText(text)
                }.onSuccess { report ->
                    toast(context, report.summary())
                }.onFailure { error ->
                    toast(context, "Импорт не удался: ${error.message ?: "ошибка"}")
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.createOrder(onOpenOrder) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(section.createLabel, fontWeight = FontWeight.SemiBold) },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Header(
                    state = state,
                    onOpenTheme = { showThemeSheet = true },
                    onExport = { exportLauncher.launch(exportFileName()) },
                    onImport = {
                        importLauncher.launch(
                            arrayOf("application/json", "text/plain", "application/octet-stream"),
                        )
                    },
                )
            }

            item {
                SectionTabs(
                    selected = section,
                    onSelect = {
                        section = it
                    },
                )
            }

            item {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(section.searchHint) },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setQuery("") }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Очистить поиск")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = ImeAction.Search,
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            }

            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatusFilterChip(
                        label = "Все",
                        selected = state.filter == null,
                        onClick = { viewModel.setFilter(null) },
                    )
                    state.statuses.forEach { status ->
                        StatusFilterChip(
                            label = status.short,
                            selected = state.filter == status,
                            onClick = { viewModel.setFilter(status) },
                        )
                    }
                }
            }

            if (!state.loaded) {
                item { LoadingLine() }
            } else if (state.visible.isEmpty()) {
                item {
                    EmptyState(
                        hasFilters = state.hasFilters,
                        emptyText = section.emptyText,
                        onReset = viewModel::resetFilters,
                    )
                }
            }

            items(state.visible, key = { it.order.id }) { card ->
                OrderRow(
                    card = card,
                    section = section,
                    onClick = { onOpenOrder(card.order.id) },
                )
            }
        }
        }

        if (showThemeSheet) {
            ThemeOverlay(
                settings = settings,
                onMode = onThemeMode,
                onAccent = onAccent,
                onDismiss = { showThemeSheet = false },
            )
        }
    }
}

/** Вкладки разделов: заказы мастера, заметки админа, брифы с диктофона, маркетинг. */
@Composable
private fun SectionTabs(selected: Section, onSelect: (Section) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .horizontalScroll(rememberScrollState())
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Section.entries.forEach { candidate ->
            val isSelected = candidate == selected
            Text(
                text = candidate.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.Transparent
                        },
                    )
                    .clickable { onSelect(candidate) }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            )
        }
    }
}

@Composable
private fun Header(
    state: OrdersUiState,
    onOpenTheme: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    val summary = if (state.cards.isEmpty()) {
        "Пока пусто"
    } else {
        buildString {
            append("в работе: ${state.activeCount} · закрыто: ${state.doneCount}")
            if (state.refusedCount > 0) append(" · архив/отказ: ${state.refusedCount}")
        }
    }

    Column(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Pets,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "meow meow записи",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onOpenTheme, modifier = Modifier.size(34.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Palette,
                    contentDescription = "Оформление",
                    modifier = Modifier.size(19.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BackupMenu(onExport = onExport, onImport = onImport)
        }
        Spacer(Modifier.height(8.dp))
        Text(text = state.section.label, style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Меню архива: вынести базу в файл или влить файл обратно. */
@Composable
private fun BackupMenu(onExport: () -> Unit, onImport: () -> Unit) {
    var open by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { open = true }, modifier = Modifier.size(34.dp)) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Архив записей",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("Экспорт в файл") },
                onClick = {
                    open = false
                    onExport()
                },
            )
            DropdownMenuItem(
                text = { Text("Импорт из файла") },
                onClick = {
                    open = false
                    onImport()
                },
            )
        }
    }
}

@Composable
private fun StatusFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        shape = RoundedCornerShape(50),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            selectedBorderColor = Color.Transparent,
        ),
    )
}

@Composable
private fun OrderRow(
    card: OrderCard,
    section: Section,
    onClick: () -> Unit,
) {
    val order = card.order
    val status = section.statusOf(order.status)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(status.tint()),
            )
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = order.model.ifBlank { "Без названия" },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (section.hasClientFields) {
                            Spacer(Modifier.height(3.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = order.clientName.ifBlank { "Клиент не указан" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    StatusPill(status)
                }

                val preview = problemPreview(order.problem)
                if (preview.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = preview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.height(9.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatWhen(order.updatedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (section.hasParts && card.partsCount > 0) {
                        MetaDot()
                        Text(
                            text = partsLabel(card.partsCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (card.photosCount > 0) {
                        MetaDot()
                        Text(
                            text = photosLabel(card.photosCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaDot() {
    Text(
        text = "·",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(horizontal = 6.dp),
    )
}

@Composable
private fun LoadingLine() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
            strokeWidth = 2.dp,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Загружаем записи…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyState(hasFilters: Boolean, emptyText: String, onReset: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(78.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Pets,
                contentDescription = null,
                modifier = Modifier.size(34.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = if (hasFilters) "Ничего не нашлось" else "Здесь пока тихо",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (hasFilters) {
                "Измените запрос или сбросьте фильтр"
            } else {
                emptyText
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        if (hasFilters) {
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onReset) { Text("Сбросить фильтры") }
        }
    }
}

/** Имя по умолчанию в диалоге сохранения: дата, чтобы архивы не путались. */
private fun exportFileName(): String =
    "meow-meow-zapisi-${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}.json"

private fun toast(context: Context, text: String) {
    Toast.makeText(context, text, Toast.LENGTH_LONG).show()
}

/** «Импорт: заказов 3, чек-листа 9, фото 1 · без файлов: 2» — по факту, а не по файлу. */
private fun ImportReport.summary(): String = buildString {
    append("Импорт: заказов $orders, чек-листа $checklist, запчастей $parts, фото $photos")
    if (photosMissing > 0) append(" · без файлов: $photosMissing")
}
