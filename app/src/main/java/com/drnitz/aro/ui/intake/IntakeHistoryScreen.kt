package com.drnitz.aro.ui.intake

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drnitz.aro.AroApplication
import com.drnitz.aro.data.repository.AroRepository
import com.drnitz.aro.domain.model.FastingSession
import com.drnitz.aro.domain.model.WaterLog
import com.drnitz.aro.domain.model.WaterLogSource
import com.drnitz.aro.theme.AroTheme
import com.drnitz.aro.ui.plan.PlanDatePickerDialog
import com.drnitz.aro.ui.plan.PlanTimePickerDialog
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * A permanent view of the two intake logs that previously vanished from the diary:
 * every glass of water and every fasting window.
 *
 * The screen reads the same Room tables the widgets write to, so opening it after
 * logging a drink or ending a fast immediately shows the new record. Deleting a water
 * entry also adjusts `days.water_ml` inside the repository transaction.
 */
@Composable
fun IntakeHistoryScreen(
    modifier: Modifier = Modifier,
    initialTab: Int = 0,
    onBack: () -> Unit = {},
    repository: AroRepository? = null,
) {
    val context = LocalContext.current
    val effectiveRepo = repository
        ?: (context.applicationContext as AroApplication).repository
    val vm: IntakeHistoryViewModel = viewModel(
        factory = IntakeHistoryViewModel.factory(effectiveRepo),
    )

    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(0, 1)) }
    LaunchedEffect(initialTab) {
        selectedTab = initialTab.coerceIn(0, 1)
    }

    val state by vm.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var waterDraft by remember { mutableStateOf<WaterLogDraft?>(null) }
    var fastingDraft by remember { mutableStateOf<FastingDraft?>(null) }

    val deleteWater: (WaterLog) -> Unit = { log ->
        vm.deleteWaterLog(log.id)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "${formatMl(log.amountMl)} water entry deleted",
                actionLabel = "Undo",
                withDismissAction = true,
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) vm.restoreWaterLog(log)
        }
    }
    val deleteFast: (FastingSession) -> Unit = { session ->
        vm.deleteFastingSession(session.id)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Fasting session deleted",
                actionLabel = "Undo",
                withDismissAction = true,
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) vm.restoreFastingSession(session)
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "Intake History",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Every drink and every fast, kept on this device",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                SecondaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Water") },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Fasting") },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                }

                when (val s = state) {
                    IntakeHistoryUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    is IntakeHistoryUiState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Unable to load intake history",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = s.throwable.message ?: "Unknown error",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }

                    is IntakeHistoryUiState.Success -> {
                        if (selectedTab == 0) {
                            WaterHistoryTab(
                                state = s,
                                onAdd = {
                                    val now = LocalDateTime.now().withSecond(0).withNano(0)
                                    waterDraft = WaterLogDraft(
                                        id = null,
                                        date = now.toLocalDate(),
                                        amountMl = 250,
                                        time = now.toLocalTime(),
                                    )
                                },
                                onEdit = { waterDraft = it.toDraft() },
                                onDelete = deleteWater,
                            )
                        } else {
                            FastingHistoryTab(
                                state = s,
                                onAdd = {
                                    val end = LocalDateTime.now().withSecond(0).withNano(0)
                                    fastingDraft = FastingDraft(
                                        id = null,
                                        targetHours = 16,
                                        start = end.minusHours(16),
                                        end = end,
                                        note = "",
                                    )
                                },
                                onEdit = { fastingDraft = it.toDraft() },
                                onDelete = deleteFast,
                            )
                        }
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp),
            )
        }
    }

    waterDraft?.let { draft ->
        WaterLogDialog(
            initial = draft,
            today = (state as? IntakeHistoryUiState.Success)?.today ?: LocalDate.now(),
            onDismiss = { waterDraft = null },
            onSave = { updated ->
                val at = updated.toInstant()
                if (updated.id == null) {
                    vm.addWaterLog(updated.date, updated.amountMl, at)
                } else {
                    vm.updateWaterLog(updated.id, updated.date, updated.amountMl, at)
                }
                waterDraft = null
            },
        )
    }

    fastingDraft?.let { draft ->
        FastingLogDialog(
            initial = draft,
            now = LocalDateTime.now(),
            onDismiss = { fastingDraft = null },
            onSave = { updated ->
                val startedAt = updated.toStartInstant()
                val endedAt = updated.toEndInstant()
                if (updated.id == null) {
                    vm.addFastingSession(
                        targetHours = updated.targetHours,
                        startedAt = startedAt,
                        endedAt = endedAt,
                        note = updated.note,
                    )
                } else {
                    vm.updateFastingSession(
                        id = updated.id,
                        targetHours = updated.targetHours,
                        startedAt = startedAt,
                        endedAt = endedAt,
                        note = updated.note,
                    )
                }
                fastingDraft = null
            },
        )
    }
}

@Composable
private fun WaterHistoryTab(
    state: IntakeHistoryUiState.Success,
    onAdd: () -> Unit,
    onEdit: (WaterLog) -> Unit,
    onDelete: (WaterLog) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (state.waterLogs.isEmpty()) {
            item("empty-water") {
                EmptyLogCard(
                    icon = Icons.Default.WaterDrop,
                    accent = AroTheme.macroColors.water,
                    title = "No water logged yet",
                    message = "Add a drink, or edit any entry later, and it will stay here " +
                        "with its exact date, time and volume.",
                    actionLabel = "Add drink",
                    onAction = onAdd,
                )
            }
        } else {
            item("water-summary") {
                WaterSummaryCard(state = state, onAdd = onAdd)
            }

            state.waterLogsByDay.forEach { (date, logs) ->
                item(key = "water-day-$date") {
                    WaterDayHeader(date = date, today = state.today, logs = logs)
                }
                items(logs, key = { it.id }) { log ->
                    WaterLogRow(
                        log = log,
                        onEdit = { onEdit(log) },
                        onDelete = { onDelete(log) },
                    )
                }
            }
        }

        item("water-bottom-space") {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun WaterSummaryCard(
    state: IntakeHistoryUiState.Success,
    onAdd: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.WaterDrop,
                    contentDescription = null,
                    tint = AroTheme.macroColors.water,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Water log",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onAdd) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${drinkCountLabel(state.waterLogs.size)} logged · " +
                    "${formatMl(state.totalWaterMl)} total",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                StatBlock(
                    value = state.todayWaterLogs.size.toString(),
                    label = "Today",
                    accent = AroTheme.macroColors.water,
                    modifier = Modifier.weight(1f),
                )
                StatBlock(
                    value = state.last7DaysWaterLogs.size.toString(),
                    label = "Last 7 days",
                    accent = AroTheme.macroColors.water,
                    modifier = Modifier.weight(1f),
                )
                StatBlock(
                    value = state.last30DaysWaterLogs.size.toString(),
                    label = "Last 30 days",
                    accent = AroTheme.macroColors.water,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun WaterDayHeader(
    date: LocalDate,
    today: LocalDate,
    logs: List<WaterLog>,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = formatDayLabel(date, today),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "${drinkCountLabel(logs.size)} · ${formatMl(logs.sumOf { it.amountMl })}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WaterLogRow(
    log: WaterLog,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 10.dp, end = 4.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.WaterDrop,
                contentDescription = null,
                tint = AroTheme.macroColors.water,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${log.amountMl} mL",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = waterLogDetail(log),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit ${log.amountMl} mL water entry",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp),
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete ${log.amountMl} mL water entry",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun FastingHistoryTab(
    state: IntakeHistoryUiState.Success,
    onAdd: () -> Unit,
    onEdit: (FastingSession) -> Unit,
    onDelete: (FastingSession) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (state.fastingSessions.isEmpty()) {
            item("empty-fast") {
                EmptyLogCard(
                    icon = Icons.Default.Timer,
                    accent = AroTheme.macroColors.fasting,
                    title = "No fasts recorded yet",
                    message = "Start a fast from the Diary, or add a completed one here. " +
                        "You can correct its window and add a note at any time.",
                    actionLabel = "Add fast",
                    onAction = onAdd,
                )
            }
        } else {
            item("fast-summary") {
                FastingSummaryCard(state = state, onAdd = onAdd)
            }
            items(state.fastingSessions, key = { it.id }) { session ->
                FastingHistoryRow(
                    session = session,
                    onEdit = { onEdit(session) },
                    onDelete = { onDelete(session) },
                )
            }
        }

        item("fast-bottom-space") {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FastingSummaryCard(
    state: IntakeHistoryUiState.Success,
    onAdd: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = AroTheme.macroColors.fasting,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Fasting log",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onAdd) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${fastCountLabel(state.fastingSessions.size)} recorded · " +
                    "${state.completedFasts.size} completed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                StatBlock(
                    value = state.targetMetCount.toString(),
                    label = "Goal met",
                    accent = AroTheme.macroColors.clinicalOptimal,
                    modifier = Modifier.weight(1f),
                )
                StatBlock(
                    value = formatDuration(state.averageCompletedMinutes),
                    label = "Avg fast",
                    accent = AroTheme.macroColors.fasting,
                    modifier = Modifier.weight(1f),
                )
                StatBlock(
                    value = formatDuration(state.totalCompletedMinutes),
                    label = "Total time",
                    accent = AroTheme.macroColors.fasting,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun FastingHistoryRow(
    session: FastingSession,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val status = fastStatus(session)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 10.dp, end = 4.dp, bottom = 10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = AroTheme.macroColors.fasting,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(18.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${session.targetHours}h fast",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusPill(label = status.label, color = status.color)
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = fastRange(session),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (session.isActive) {
                        "Still running · target ${session.targetHours}h"
                    } else {
                        "Duration ${formatDuration(session.durationMinutes())} · " +
                            "target ${session.targetHours}h"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                )
                if (session.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = session.note,
                        style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (!session.isActive) {
                IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit fast from ${formatDayLabel(session.date, LocalDate.now())}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp),
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete fast from ${formatDayLabel(session.date, LocalDate.now())}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatBlock(
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = accent,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyLogCard(
    icon: ImageVector,
    accent: Color,
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(38.dp),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(onClick = onAction) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun StatusPill(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

private data class FastStatus(val label: String, val color: Color)

@Composable
private fun fastStatus(session: FastingSession): FastStatus = when {
    session.isActive && session.metTarget() ->
        FastStatus("Goal reached", AroTheme.macroColors.clinicalOptimal)
    session.isActive ->
        FastStatus("In progress", AroTheme.macroColors.fasting)
    session.metTarget() ->
        FastStatus("Goal met", AroTheme.macroColors.clinicalOptimal)
    else ->
        FastStatus("Ended early", AroTheme.macroColors.clinicalModerate)
}

private data class WaterLogDraft(
    val id: String?,
    val date: LocalDate,
    val amountMl: Int,
    val time: LocalTime,
) {
    fun toInstant(): Instant = date.atTime(time).atZone(ZoneId.systemDefault()).toInstant()
}

private fun WaterLog.toDraft(): WaterLogDraft = WaterLogDraft(
    id = id,
    date = date,
    amountMl = amountMl,
    time = loggedAt.atZone(ZoneId.systemDefault()).toLocalTime().withSecond(0).withNano(0),
)

private data class FastingDraft(
    val id: String?,
    val targetHours: Int,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val note: String,
) {
    fun toStartInstant(): Instant = start.atZone(ZoneId.systemDefault()).toInstant()

    fun toEndInstant(): Instant = end.atZone(ZoneId.systemDefault()).toInstant()
}

private fun FastingSession.toDraft(): FastingDraft {
    val zone = ZoneId.systemDefault()
    val end = endedAt?.atZone(zone)?.toLocalDateTime() ?: LocalDateTime.now()
    return FastingDraft(
        id = id,
        targetHours = targetHours,
        start = startedAt.atZone(zone).toLocalDateTime().withSecond(0).withNano(0),
        end = end.withSecond(0).withNano(0),
        note = note,
    )
}

@Composable
private fun WaterLogDialog(
    initial: WaterLogDraft,
    today: LocalDate,
    onDismiss: () -> Unit,
    onSave: (WaterLogDraft) -> Unit,
) {
    var amountText by remember(initial.id) { mutableStateOf(initial.amountMl.toString()) }
    var date by remember(initial.id) { mutableStateOf(initial.date) }
    var time by remember(initial.id) { mutableStateOf(initial.time) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val amount = amountText.toIntOrNull()
    val isFuture = date.isAfter(today)
    val canSave = amount != null && amount in 1..10_000 && !isFuture

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == null) "Add a drink" else "Edit drink") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        if (input.length <= 5 && input.all(Char::isDigit)) amountText = input
                    },
                    label = { Text("Amount (mL)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = amountText.isNotEmpty() && (amount == null || amount <= 0),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(100, 250, 500, 750, 1000).forEach { preset ->
                        AssistChip(
                            onClick = { amountText = preset.toString() },
                            label = { Text(preset.toString()) },
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = formatDayLabel(date, today),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(0.7f),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(time.format(TIME_FORMAT))
                    }
                }
                if (isFuture) {
                    Text(
                        text = "Choose today or an earlier date.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        initial.copy(
                            amountMl = amount ?: initial.amountMl,
                            date = date,
                            time = time,
                        ),
                    )
                },
                enabled = canSave,
            ) {
                Text(if (initial.id == null) "Add" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )

    if (showDatePicker) {
        PlanDatePickerDialog(
            initialDate = date,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                date = it
                showDatePicker = false
            },
            title = "Drink date",
            confirmLabel = "Use date",
        )
    }
    if (showTimePicker) {
        PlanTimePickerDialog(
            initial = time,
            onDismiss = { showTimePicker = false },
            onConfirm = {
                time = it
                showTimePicker = false
            },
            title = "Drink time",
            confirmLabel = "Set time",
        )
    }
}

@Composable
private fun FastingLogDialog(
    initial: FastingDraft,
    now: LocalDateTime,
    onDismiss: () -> Unit,
    onSave: (FastingDraft) -> Unit,
) {
    var targetText by remember(initial.id) { mutableStateOf(initial.targetHours.toString()) }
    var startDate by remember(initial.id) { mutableStateOf(initial.start.toLocalDate()) }
    var startTime by remember(initial.id) { mutableStateOf(initial.start.toLocalTime()) }
    var endDate by remember(initial.id) { mutableStateOf(initial.end.toLocalDate()) }
    var endTime by remember(initial.id) { mutableStateOf(initial.end.toLocalTime()) }
    var note by remember(initial.id) { mutableStateOf(initial.note) }
    var showStartDate by remember { mutableStateOf(false) }
    var showStartTime by remember { mutableStateOf(false) }
    var showEndDate by remember { mutableStateOf(false) }
    var showEndTime by remember { mutableStateOf(false) }

    val target = targetText.toIntOrNull()
    val targetValid = target != null && target in 1..72
    val start = LocalDateTime.of(startDate, startTime)
    val end = LocalDateTime.of(endDate, endTime)
    val durationMinutes = Duration.between(start, end).toMinutes()
    val orderValid = end.isAfter(start)
    val endNotFuture = !end.isAfter(now)
    val canSave = targetValid && orderValid && endNotFuture

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == null) "Add a fast" else "Edit fast") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { input ->
                        if (input.length <= 2 && input.all(Char::isDigit)) targetText = input
                    },
                    label = { Text("Target (hours)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = targetText.isNotEmpty() && !targetValid,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(16, 18, 20, 24).forEach { preset ->
                        AssistChip(
                            onClick = { targetText = preset.toString() },
                            label = { Text("${preset}h") },
                        )
                    }
                }
                FastDateTimeRow(
                    label = "Started",
                    date = startDate,
                    time = startTime,
                    onDateClick = { showStartDate = true },
                    onTimeClick = { showStartTime = true },
                )
                FastDateTimeRow(
                    label = "Ended",
                    date = endDate,
                    time = endTime,
                    onDateClick = { showEndDate = true },
                    onTimeClick = { showEndTime = true },
                )
                Text(
                    text = "Window ${formatDuration(if (orderValid) durationMinutes else 0)}" +
                        if (targetValid && orderValid) " · target ${target}h" else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= 500) note = it },
                    label = { Text("Note (optional)") },
                    placeholder = { Text("Hunger, energy, electrolytes…") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                when {
                    targetText.isNotEmpty() && !targetValid -> ValidationHint(
                        "Target must be between 1 and 72 hours.",
                    )
                    !orderValid -> ValidationHint("The end must be after the start.")
                    !endNotFuture -> ValidationHint("The end can't be in the future.")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        initial.copy(
                            targetHours = target ?: initial.targetHours,
                            start = start,
                            end = end,
                            note = note.trim(),
                        ),
                    )
                },
                enabled = canSave,
            ) {
                Text(if (initial.id == null) "Add" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )

    if (showStartDate) {
        PlanDatePickerDialog(
            initialDate = startDate,
            onDismiss = { showStartDate = false },
            onConfirm = {
                startDate = it
                showStartDate = false
            },
            title = "Fast start date",
        )
    }
    if (showStartTime) {
        PlanTimePickerDialog(
            initial = startTime,
            onDismiss = { showStartTime = false },
            onConfirm = {
                startTime = it
                showStartTime = false
            },
            title = "Fast start time",
        )
    }
    if (showEndDate) {
        PlanDatePickerDialog(
            initialDate = endDate,
            onDismiss = { showEndDate = false },
            onConfirm = {
                endDate = it
                showEndDate = false
            },
            title = "Fast end date",
        )
    }
    if (showEndTime) {
        PlanTimePickerDialog(
            initial = endTime,
            onDismiss = { showEndTime = false },
            onConfirm = {
                endTime = it
                showEndTime = false
            },
            title = "Fast end time",
        )
    }
}

@Composable
private fun FastDateTimeRow(
    label: String,
    date: LocalDate,
    time: LocalTime,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onDateClick,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = formatCompactDate(date),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            OutlinedButton(
                onClick = onTimeClick,
                modifier = Modifier.weight(0.7f),
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(time.format(TIME_FORMAT))
            }
        }
    }
}

@Composable
private fun ValidationHint(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.error,
    )
}

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val SHORT_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")
private val FULL_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")
private val SHORT_DATE_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, HH:mm")
private val FULL_DATE_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm")

private fun formatDayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    else -> if (date.year == today.year) {
        date.format(SHORT_DATE_FORMAT)
    } else {
        date.format(FULL_DATE_FORMAT)
    }
}

private fun formatCompactDate(date: LocalDate): String =
    if (date.year == LocalDate.now().year) {
        date.format(SHORT_DATE_FORMAT)
    } else {
        date.format(FULL_DATE_FORMAT)
    }

private fun formatMl(amountMl: Int): String = when {
    amountMl >= 1000 && amountMl % 1000 == 0 -> "${amountMl / 1000} L"
    amountMl >= 1000 -> String.format(Locale.getDefault(), "%.1f L", amountMl / 1000.0)
    else -> "$amountMl mL"
}

private fun drinkCountLabel(count: Int): String =
    if (count == 1) "1 drink" else "$count drinks"

private fun fastCountLabel(count: Int): String =
    if (count == 1) "1 fast" else "$count fasts"

private fun formatDuration(totalMinutes: Long): String {
    val safeMinutes = totalMinutes.coerceAtLeast(0)
    val hours = safeMinutes / 60
    val minutes = safeMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}

private fun waterLogDetail(log: WaterLog): String {
    val time = log.loggedAt.atZone(ZoneId.systemDefault()).format(TIME_FORMAT)
    return when (log.source) {
        WaterLogSource.BACKFILL -> "Imported from an earlier daily total"
        WaterLogSource.CORRECTION -> "Set manually at $time"
        WaterLogSource.MANUAL -> "Logged at $time"
    }
}

private fun fastRange(session: FastingSession): String {
    val zone = ZoneId.systemDefault()
    val start = session.startedAt.atZone(zone)
    val end = session.endedAt?.atZone(zone)
        ?: return "Started ${formatDateTime(start)}"
    return if (start.toLocalDate() == end.toLocalDate()) {
        "${datePatternFor(start)} · ${start.format(TIME_FORMAT)} → ${end.format(TIME_FORMAT)}"
    } else {
        "${formatDateTime(start)} → ${formatDateTime(end)}"
    }
}

private fun datePatternFor(time: ZonedDateTime): String =
    if (time.year == LocalDate.now().year) {
        time.format(SHORT_DATE_FORMAT)
    } else {
        time.format(FULL_DATE_FORMAT)
    }

private fun formatDateTime(time: ZonedDateTime): String =
    if (time.year == LocalDate.now().year) {
        time.format(SHORT_DATE_TIME_FORMAT)
    } else {
        time.format(FULL_DATE_TIME_FORMAT)
    }
