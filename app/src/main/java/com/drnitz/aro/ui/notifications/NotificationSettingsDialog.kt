package com.drnitz.aro.ui.notifications

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.drnitz.aro.notifications.AroNotificationScheduler
import com.drnitz.aro.notifications.MedicationReminder
import com.drnitz.aro.notifications.NotificationHelper
import com.drnitz.aro.notifications.NotificationPreferences
import com.drnitz.aro.theme.AroTheme
import java.util.Locale

@Composable
fun NotificationSettingsDialog(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { NotificationPreferences(context) }

    var hydrationEnabled by remember { mutableStateOf(prefs.hydrationEnabled) }
    var hydrationInterval by remember { mutableIntStateOf(prefs.hydrationIntervalHours) }
    var hydrationStartHour by remember { mutableIntStateOf(prefs.hydrationStartHour) }
    var hydrationEndHour by remember { mutableIntStateOf(prefs.hydrationEndHour) }

    var fastingEnabled by remember { mutableStateOf(prefs.fastingEnabled) }

    var medicationList by remember { mutableStateOf(prefs.getMedicationReminders()) }
    var showAddMedDialog by remember { mutableStateOf(false) }

    // Android 13+ Notification Permission State
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            },
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Notification Settings",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "100% Offline Local Reminders",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Permission Banner if not granted on Android 13+
                    if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                                ),
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(24.dp),
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Permission Required",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                        )
                                        Text(
                                            text = "Allow notifications to receive hydration, fasting, medication, and planned-item alerts.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f),
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error,
                                            contentColor = MaterialTheme.colorScheme.onError,
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    ) {
                                        Text(text = "Grant", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }

                    // 1. Hydration Reminders Card
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            ),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.WaterDrop,
                                            contentDescription = null,
                                            tint = AroTheme.macroColors.water,
                                            modifier = Modifier.size(20.dp),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Hydration Reminders",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                            Text(
                                                text = "Lock-screen quick '+250 mL' logger",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }

                                    Switch(
                                        checked = hydrationEnabled,
                                        onCheckedChange = { checked ->
                                            hydrationEnabled = checked
                                            prefs.hydrationEnabled = checked
                                            if (checked) {
                                                AroNotificationScheduler.scheduleHydrationReminders(context)
                                            } else {
                                                AroNotificationScheduler.cancelHydrationReminders(context)
                                            }
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = AroTheme.macroColors.water,
                                            checkedTrackColor = AroTheme.macroColors.waterBg,
                                        ),
                                    )
                                }

                                if (hydrationEnabled) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Reminder Interval",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        listOf(1, 2, 3, 4).forEach { hours ->
                                            FilterChip(
                                                selected = hydrationInterval == hours,
                                                onClick = {
                                                    hydrationInterval = hours
                                                    prefs.hydrationIntervalHours = hours
                                                    AroNotificationScheduler.scheduleHydrationReminders(context)
                                                },
                                                label = { Text("Every ${hours}h") },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = AroTheme.macroColors.waterBg,
                                                    selectedLabelColor = AroTheme.macroColors.water,
                                                ),
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = "Active Hours: %02d:00 - %02d:00".format(Locale.US, hydrationStartHour, hydrationEndHour),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )

                                        OutlinedButton(
                                            onClick = {
                                                NotificationHelper.showHydrationNotification(context)
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        ) {
                                            Text("Test Alert", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Intermittent Fasting Reminders Card
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            ),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Timer,
                                            contentDescription = null,
                                            tint = AroTheme.macroColors.fasting,
                                            modifier = Modifier.size(20.dp),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Fasting Target Alerts",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                            Text(
                                                text = "Alerts when goal is reached with 'End Fast'",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }

                                    Switch(
                                        checked = fastingEnabled,
                                        onCheckedChange = { checked ->
                                            fastingEnabled = checked
                                            prefs.fastingEnabled = checked
                                            if (!checked) {
                                                AroNotificationScheduler.cancelFastingCompletion(context)
                                            }
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = AroTheme.macroColors.fasting,
                                            checkedTrackColor = AroTheme.macroColors.fasting.copy(alpha = 0.3f),
                                        ),
                                    )
                                }

                                if (fastingEnabled) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = "Notifies on lock screen when target hours elapse.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        OutlinedButton(
                                            onClick = {
                                                NotificationHelper.showFastingNotification(context, 16)
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        ) {
                                            Text("Test Alert", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Medications & Supplements Reminders Card
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            ),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Medication,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981), // Emerald
                                            modifier = Modifier.size(20.dp),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Medications & Supplements",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                            Text(
                                                text = "Daily timed alerts with 'Mark Taken' button",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = { showAddMedDialog = true },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF10B981),
                                            contentColor = Color.White,
                                        ),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Add", style = MaterialTheme.typography.labelMedium)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                if (medicationList.isEmpty()) {
                                    Text(
                                        text = "No medication or supplement reminders configured. Tap Add above to schedule vitamins, minerals, or prescription alerts.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 8.dp),
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        medicationList.forEach { med ->
                                            MedicationItemRow(
                                                reminder = med,
                                                onToggle = { enabled ->
                                                    val updated = med.copy(enabled = enabled)
                                                    prefs.addMedicationReminder(updated)
                                                    medicationList = prefs.getMedicationReminders()
                                                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                                                    if (alarmManager != null) {
                                                        if (enabled) {
                                                            AroNotificationScheduler.scheduleSingleMedication(context, alarmManager, updated)
                                                        } else {
                                                            AroNotificationScheduler.cancelMedicationReminder(context, updated.id)
                                                        }
                                                    }
                                                },
                                                onDelete = {
                                                    prefs.deleteMedicationReminder(med.id)
                                                    medicationList = prefs.getMedicationReminders()
                                                    AroNotificationScheduler.cancelMedicationReminder(context, med.id)
                                                },
                                                onTest = {
                                                    NotificationHelper.showMedicationNotification(
                                                        context = context,
                                                        medId = med.id,
                                                        name = med.name,
                                                        dosage = med.dosage,
                                                    )
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. Planned Item Reminders Info Card
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = Color(0xFF6366F1), // Indigo
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Planned reminders",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = "Each planned meal, workout, or pill uses its own time and repeat schedule. Open a day's plan to turn its reminder on or off.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    // 5. Exact Alarm Engine Info Banner
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Offline & Doze-Resistant Engine",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = "Powered by AlarmManager setExactAndAllowWhileIdle and BootCompletedReceiver. Zero internet required.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Done Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Done", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }

    if (showAddMedDialog) {
        AddMedicationDialog(
            onDismiss = { showAddMedDialog = false },
            onSave = { name, dosage, hour, minute ->
                val newMed = MedicationReminder(
                    name = name,
                    dosage = dosage,
                    hour = hour,
                    minute = minute,
                    enabled = true,
                )
                prefs.addMedicationReminder(newMed)
                medicationList = prefs.getMedicationReminders()
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                if (alarmManager != null) {
                    AroNotificationScheduler.scheduleSingleMedication(context, alarmManager, newMed)
                }
                showAddMedDialog = false
            },
        )
    }
}

@Composable
private fun MedicationItemRow(
    reminder: MedicationReminder,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onTest: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val timeStr = "%02d:%02d".format(Locale.US, reminder.hour, reminder.minute)
                val dosageStr = if (reminder.dosage.isNotBlank()) " • ${reminder.dosage}" else ""
                Text(
                    text = "$timeStr$dosageStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = onTest,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp),
                ) {
                    Text("Test", style = MaterialTheme.typography.labelSmall)
                }

                Spacer(modifier = Modifier.width(6.dp))

                Switch(
                    checked = reminder.enabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.size(width = 40.dp, height = 24.dp),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF10B981),
                        checkedTrackColor = Color(0xFF10B981).copy(alpha = 0.3f),
                    ),
                )

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Reminder",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AddMedicationDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, dosage: String, hour: Int, minute: Int) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var hour by remember { mutableIntStateOf(8) }
    var minute by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Reminder",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (e.g. Vitamin D3, Creatine)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = { Text("Dosage / Instructions (optional)") },
                    placeholder = { Text("e.g. 5000 IU with breakfast") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = "Time of Day",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        "08:00 (Morning)" to Pair(8, 0),
                        "12:00 (Noon)" to Pair(12, 0),
                        "18:00 (Dinner)" to Pair(18, 0),
                        "21:00 (Night)" to Pair(21, 0),
                    ).forEach { (label, time) ->
                        FilterChip(
                            selected = hour == time.first && minute == time.second,
                            onClick = {
                                hour = time.first
                                minute = time.second
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = "%02d".format(Locale.US, hour),
                        onValueChange = { text ->
                            text.toIntOrNull()?.let { hour = it.coerceIn(0, 23) }
                        },
                        label = { Text("Hour (0-23)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )

                    OutlinedTextField(
                        value = "%02d".format(Locale.US, minute),
                        onValueChange = { text ->
                            text.toIntOrNull()?.let { minute = it.coerceIn(0, 59) }
                        },
                        label = { Text("Min (0-59)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim(), dosage.trim(), hour, minute)
                    }
                },
                enabled = name.isNotBlank(),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
