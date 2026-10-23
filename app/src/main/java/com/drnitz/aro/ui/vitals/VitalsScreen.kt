package com.drnitz.aro.ui.vitals

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drnitz.aro.AroApplication
import com.drnitz.aro.data.repository.AroRepository
import com.drnitz.aro.domain.model.BloodGlucose
import com.drnitz.aro.domain.model.BloodPressure
import com.drnitz.aro.domain.model.BloodPressureStage
import com.drnitz.aro.domain.model.DoctorConsultation
import com.drnitz.aro.domain.model.GlucoseCategory
import com.drnitz.aro.domain.model.GlucoseMeasurementContext
import com.drnitz.aro.domain.model.MedicationAdherence
import com.drnitz.aro.domain.model.VitalsEntry
import com.drnitz.aro.export.ClinicalPdfGenerator
import com.drnitz.aro.notifications.NotificationPreferences
import com.drnitz.aro.theme.AroTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun VitalsScreen(
    initialTab: Int = 0,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    repository: AroRepository? = null,
) {
    val context = LocalContext.current
    val effectiveRepo = repository ?: (context.applicationContext as AroApplication).repository
    val notificationPreferences = remember { NotificationPreferences(context) }
    val vm: VitalsViewModel = viewModel(
        factory = VitalsViewModel.factory(
            effectiveRepo,
            notificationSettings = notificationPreferences.settings,
        ),
    )

    LaunchedEffect(initialTab) {
        vm.selectTab(initialTab)
    }

    val state by vm.uiState.collectAsStateWithLifecycle()
    val selectedTab by vm.selectedTab.collectAsStateWithLifecycle()

    var showLogVitalsDialog by remember { mutableStateOf(false) }
    var showLogConsultDialog by remember { mutableStateOf(false) }
    var isExportingPdf by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
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
                        text = "Clinical Records & Vitals",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Patient Vitals, Doctor Notes & PDF Export",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Tab Navigation Row
            SecondaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { vm.selectTab(0) },
                    text = { Text("Patient Vitals") },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { vm.selectTab(1) },
                    text = { Text("Doctor Visits") },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.MedicalServices,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { vm.selectTab(2) },
                    text = { Text("PDF Export") },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
            }

            when (val s = state) {
                VitalsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is VitalsUiState.Success -> {
                    when (selectedTab) {
                        0 -> VitalsTabContent(
                            vitals = s.vitals,
                            latest = s.latestVitals,
                            onOpenLogVitals = { showLogVitalsDialog = true },
                            onDeleteVitals = vm::deleteVitals,
                        )
                        1 -> ConsultationsTabContent(
                            consultations = s.consultations,
                            adherence = s.medicationAdherence,
                            onOpenLogConsult = { showLogConsultDialog = true },
                            onDeleteConsult = vm::deleteConsultation,
                        )
                        2 -> PdfExportTabContent(
                            state = s,
                            isExporting = isExportingPdf,
                            onExport = { rangeDays ->
                                isExportingPdf = true
                                vm.exportPdf(context, rangeDays) { file ->
                                    isExportingPdf = false
                                    ClinicalPdfGenerator.shareReport(context, file)
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    if (showLogVitalsDialog) {
        LogVitalsDialog(
            today = vm.today,
            onDismiss = { showLogVitalsDialog = false },
            onSave = { entry ->
                vm.logVitals(entry)
                showLogVitalsDialog = false
            },
        )
    }

    if (showLogConsultDialog) {
        LogConsultationDialog(
            today = vm.today,
            onDismiss = { showLogConsultDialog = false },
            onSave = { consult ->
                vm.logConsultation(consult)
                showLogConsultDialog = false
            },
        )
    }
}

@Composable
private fun VitalsTabContent(
    vitals: List<VitalsEntry>,
    latest: VitalsEntry?,
    onOpenLogVitals: () -> Unit,
    onDeleteVitals: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Quick Action Button
        item {
            Button(
                onClick = onOpenLogVitals,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Patient Vitals", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }
        }

        // Summary Card of Most Recent Reading
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "LATEST CLINICAL SNAPSHOT",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (latest == null) {
                        Text(
                            text = "No vitals recorded yet. Tap above to log blood pressure, glucose, or pulse.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                val bp = latest.bloodPressure
                                if (bp != null) {
                                    Text(
                                        text = "${bp.systolic} / ${bp.diastolic} mmHg",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    val map = bp.meanArterialPressure.toInt()
                                    val pulseStr = bp.pulse?.let { " • $it bpm" } ?: ""
                                    Text(
                                        text = "MAP: $map mmHg$pulseStr",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                } else {
                                    Text(
                                        text = "BP Unrecorded",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            latest.bloodPressure?.let { bp ->
                                val stageColor = when (bp.stage) {
                                    BloodPressureStage.NORMAL -> Color(0xFF10B981)
                                    BloodPressureStage.ELEVATED -> Color(0xFFF59E0B)
                                    BloodPressureStage.STAGE_1_HYPERTENSION -> Color(0xFFF97316)
                                    BloodPressureStage.STAGE_2_HYPERTENSION -> Color(0xFFEF4444)
                                    BloodPressureStage.HYPERTENSIVE_CRISIS -> Color(0xFFDC2626)
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = stageColor.copy(alpha = 0.15f),
                                ) {
                                    Text(
                                        text = bp.stage.label,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = stageColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }

                        if (latest.bloodGlucose != null || latest.spo2Percent != null || latest.bodyTemperatureC != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                latest.bloodGlucose?.let { bg ->
                                    Column {
                                        Text(text = "Glucose", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = "%.0f mg/dL".format(Locale.US, bg.valueMgDl),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }

                                latest.spo2Percent?.let { spo2 ->
                                    Column {
                                        Text(text = "SpO2", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = "$spo2%",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }

                                latest.bodyTemperatureC?.let { temp ->
                                    Column {
                                        Text(text = "Temp", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = "%.1f°C".format(Locale.US, temp),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Vitals History List Header
        item {
            Text(
                text = "VITALS HISTORY (${vitals.size})",
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp),
            )
        }

        if (vitals.isEmpty()) {
            item {
                Text(
                    text = "No recorded history yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        } else {
            items(vitals) { entry ->
                VitalsHistoryCard(entry = entry, onDelete = { onDeleteVitals(entry.id) })
            }
        }
    }
}

@Composable
private fun VitalsHistoryCard(
    entry: VitalsEntry,
    onDelete: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = entry.date.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )

                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            entry.bloodPressure?.let { bp ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "BP: ${bp.systolic}/${bp.diastolic} mmHg ${bp.pulse?.let { "• $it bpm" } ?: ""}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = bp.stage.label,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            entry.bloodGlucose?.let { bg ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Glucose: %.0f mg/dL (%.1f mmol/L) • %s".format(Locale.US, bg.valueMgDl, bg.valueMmolL, bg.context.label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val extras = buildList {
                entry.ketonesMmolL?.let { add("Ketones: %.1f mmol/L".format(Locale.US, it)) }
                entry.spo2Percent?.let { add("SpO2: $it%") }
                entry.bodyTemperatureC?.let { add("Temp: %.1f°C".format(Locale.US, it)) }
            }
            if (extras.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = extras.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (entry.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Note: ${entry.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                )
            }
        }
    }
}

@Composable
private fun ConsultationsTabContent(
    consultations: List<DoctorConsultation>,
    adherence: MedicationAdherence,
    onOpenLogConsult: () -> Unit,
    onDeleteConsult: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Button(
                onClick = onOpenLogConsult,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981), // Emerald
                    contentColor = Color.White,
                ),
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Clinical Consultation", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }
        }

        if (adherence.hasData) {
            item { MedicationAdherenceCard(adherence) }
        }

        item {
            Text(
                text = "DOCTOR VISITS & NOTES (${consultations.size})",
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp),
            )
        }

        if (consultations.isEmpty()) {
            item {
                Text(
                    text = "No clinician consultation records logged. Tap above to add visit notes, diagnoses, and medication plans.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        } else {
            items(consultations) { consult ->
                ConsultationCard(consultation = consult, onDelete = { onDeleteConsult(consult.id) })
            }
        }
    }
}

@Composable
private fun MedicationAdherenceCard(adherence: MedicationAdherence) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
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
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Medication Adherence",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Last ${adherence.windowDays} days",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Text(
                    text = "${adherence.overallPercent}%",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF10B981),
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { adherence.overallPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = Color(0xFF10B981),
                trackColor = Color(0xFF10B981).copy(alpha = 0.15f),
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${adherence.takenDoses} of ${adherence.expectedDoses} expected doses logged",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            adherence.entries.forEach { entry ->
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "${entry.takenDays} of ${entry.expectedDays} days" +
                                if (entry.dosage.isNotBlank()) " • ${entry.dosage}" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = "${entry.percent}%",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (entry.percent >= 80) Color(0xFF10B981) else Color(0xFFF59E0B),
                    )
                }
            }
        }
    }
}

@Composable
private fun ConsultationCard(
    consultation: DoctorConsultation,
    onDelete: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = consultation.doctorName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${consultation.specialty} • ${consultation.date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Reason: ${consultation.reasonForVisit}",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Assessment: ${consultation.diagnosisNotes}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (consultation.prescribedMedications.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "Prescriptions: ${consultation.prescribedMedications}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (consultation.followUpPlan.isNotBlank()) {
                            Text(
                                text = "Follow-up: ${consultation.followUpPlan}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfExportTabContent(
    state: VitalsUiState.Success,
    isExporting: Boolean,
    onExport: (rangeDays: Int) -> Unit,
) {
    var selectedRangeDays by remember { mutableIntStateOf(30) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Clinical PDF Report Exporter",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "100% Offline • Doctor & Specialist Ready",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Generates a clean, multi-page clinical document formatted like a standard medical chart. Includes baseline demographics, vitals history with AHA stages, daily macronutrient averages, Aro Clinical Audit findings, and consultation logs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Date Range Selector
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Select Export Timeframe",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(
                            7 to "7 Days",
                            30 to "30 Days",
                            90 to "90 Days",
                            0 to "All History",
                        ).forEach { (days, label) ->
                            FilterChip(
                                selected = selectedRangeDays == days,
                                onClick = { selectedRangeDays = days },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                            )
                        }
                    }
                }
            }
        }

        // Summary of Included Metrics
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Report Contents Overview",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val vitalsCount = state.vitals.size
                    val consultsCount = state.consultations.size
                    val naKText = state.molarNaKRatio?.let { "%.2f".format(Locale.US, it) } ?: "—"

                    Text(text = "• Patient Profile: ${state.profile.name} (${state.profile.ageYears}y, ${state.profile.sex.name})", style = MaterialTheme.typography.bodySmall)
                    Text(text = "• Vitals Entries: $vitalsCount readings", style = MaterialTheme.typography.bodySmall)
                    Text(text = "• Clinician Visits: $consultsCount recorded sessions", style = MaterialTheme.typography.bodySmall)
                    Text(text = "• Molar Na:K Balance: $naKText", style = MaterialTheme.typography.bodySmall)
                    Text(text = "• Aro Clinical Audit: ${state.clinicalFindings.size} clinical points", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Generate & Share Button
        item {
            Button(
                onClick = { onExport(selectedRangeDays) },
                enabled = !isExporting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                if (isExporting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generating PDF...")
                } else {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate & Share Clinical PDF", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

@Composable
private fun LogVitalsDialog(
    today: LocalDate = LocalDate.now(),
    onDismiss: () -> Unit,
    onSave: (VitalsEntry) -> Unit,
) {
    var systolicText by remember { mutableStateOf("120") }
    var diastolicText by remember { mutableStateOf("80") }
    var pulseText by remember { mutableStateOf("72") }
    var glucoseText by remember { mutableStateOf("") }
    var glucoseContext by remember { mutableStateOf(GlucoseMeasurementContext.FASTING) }
    var spo2Text by remember { mutableStateOf("") }
    var tempText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

    val sys = systolicText.toIntOrNull()
    val dia = diastolicText.toIntOrNull()
    val pulse = pulseText.toIntOrNull()

    val liveStage = if (sys != null && dia != null && sys in 50..300 && dia in 30..200) {
        try {
            BloodPressure(sys, dia).stage
        } catch (_: Exception) {
            null
        }
    } else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Log Patient Vitals", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Text(
                        text = "Blood Pressure & Pulse",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = systolicText,
                            onValueChange = { systolicText = it },
                            label = { Text("Sys (mmHg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = diastolicText,
                            onValueChange = { diastolicText = it },
                            label = { Text("Dia (mmHg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = pulseText,
                            onValueChange = { pulseText = it },
                            label = { Text("Pulse (bpm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                if (liveStage != null) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "AHA/ACC Stage: ${liveStage.label}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Blood Glucose (optional)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                item {
                    OutlinedTextField(
                        value = glucoseText,
                        onValueChange = { glucoseText = it },
                        label = { Text("Glucose (mg/dL)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        GlucoseMeasurementContext.entries.forEach { ctx ->
                            FilterChip(
                                selected = glucoseContext == ctx,
                                onClick = { glucoseContext = ctx },
                                label = { Text(ctx.name.take(4)) },
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Other Vitals (optional)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = spo2Text,
                            onValueChange = { spo2Text = it },
                            label = { Text("SpO2 (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = tempText,
                            onValueChange = { tempText = it },
                            label = { Text("Temp (°C)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("Clinical Notes / Context") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bp = if (sys != null && dia != null && sys in 50..300 && dia in 30..200) {
                        try {
                            BloodPressure(sys, dia, pulse?.coerceIn(30, 250))
                        } catch (_: Exception) {
                            null
                        }
                    } else null

                    val bgVal = glucoseText.toDoubleOrNull()
                    val bg = if (bgVal != null && bgVal in 20.0..800.0) {
                        BloodGlucose(bgVal, glucoseContext)
                    } else null

                    val spo2 = spo2Text.toIntOrNull()?.coerceIn(50, 100)
                    val temp = tempText.toDoubleOrNull()?.coerceIn(30.0, 45.0)

                    val entry = VitalsEntry(
                        date = today,
                        bloodPressure = bp,
                        bloodGlucose = bg,
                        spo2Percent = spo2,
                        bodyTemperatureC = temp,
                        notes = notesText.trim(),
                    )
                    onSave(entry)
                },
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

@Composable
private fun LogConsultationDialog(
    today: LocalDate = LocalDate.now(),
    onDismiss: () -> Unit,
    onSave: (DoctorConsultation) -> Unit,
) {
    var doctorName by remember { mutableStateOf("") }
    var specialty by remember { mutableStateOf("Internal Medicine") }
    var reason by remember { mutableStateOf("") }
    var diagnosis by remember { mutableStateOf("") }
    var prescriptions by remember { mutableStateOf("") }
    var followUp by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Log Doctor Consultation", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    OutlinedTextField(
                        value = doctorName,
                        onValueChange = { doctorName = it },
                        label = { Text("Physician Name (e.g. Dr. Jenkins)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    OutlinedTextField(
                        value = specialty,
                        onValueChange = { specialty = it },
                        label = { Text("Specialty (e.g. Cardiology, GP)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        listOf("Cardiology", "Endocrinology", "GP", "Nephrology").forEach { spec ->
                            FilterChip(
                                selected = specialty == spec,
                                onClick = { specialty = spec },
                                label = { Text(spec, style = MaterialTheme.typography.labelSmall) },
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Reason for Visit / Complaint") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    OutlinedTextField(
                        value = diagnosis,
                        onValueChange = { diagnosis = it },
                        label = { Text("Clinical Assessment / Diagnosis") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    OutlinedTextField(
                        value = prescriptions,
                        onValueChange = { prescriptions = it },
                        label = { Text("Prescribed Medications & Dosages") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    OutlinedTextField(
                        value = followUp,
                        onValueChange = { followUp = it },
                        label = { Text("Follow-up Plan & Lab Orders") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (doctorName.isNotBlank() && reason.isNotBlank()) {
                        val consult = DoctorConsultation(
                            date = today,
                            doctorName = doctorName.trim(),
                            specialty = specialty.trim(),
                            reasonForVisit = reason.trim(),
                            diagnosisNotes = diagnosis.trim(),
                            prescribedMedications = prescriptions.trim(),
                            followUpPlan = followUp.trim(),
                        )
                        onSave(consult)
                    }
                },
                enabled = doctorName.isNotBlank() && reason.isNotBlank(),
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
