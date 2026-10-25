package com.drnitz.aro.ui.planner

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drnitz.aro.AroApplication
import com.drnitz.aro.data.db.AroDatabase
import com.drnitz.aro.data.repository.RoomAroRepository
import com.drnitz.aro.domain.calculation.BmiResult
import com.drnitz.aro.domain.calculation.EnergyPlan
import com.drnitz.aro.domain.clinical.AuditStatus
import com.drnitz.aro.domain.clinical.ClinicalAssessment
import com.drnitz.aro.domain.clinical.ClinicalFinding
import com.drnitz.aro.domain.clinical.FindingSeverity
import com.drnitz.aro.domain.clinical.NaKRatio
import com.drnitz.aro.domain.clinical.NutrientVerdict
import com.drnitz.aro.domain.clinical.RatioStatus
import com.drnitz.aro.domain.model.ActivityLevel
import com.drnitz.aro.domain.model.ClinicalCatalog
import com.drnitz.aro.domain.model.ClinicalProtocol
import com.drnitz.aro.domain.model.Goal
import com.drnitz.aro.domain.model.MacroPreset
import com.drnitz.aro.domain.model.Sex
import com.drnitz.aro.domain.model.UserProfile
import com.drnitz.aro.theme.AroClinicalCeiling
import com.drnitz.aro.theme.AroClinicalHighRisk
import com.drnitz.aro.theme.AroClinicalModerate
import com.drnitz.aro.theme.AroClinicalOptimal
import com.drnitz.aro.theme.AroTheme
import com.drnitz.aro.ui.components.AroDatePickerBar
import java.time.LocalDate
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyPlannerScreen(
    modifier: Modifier = Modifier,
    initialTab: Int = 0,
    onBack: () -> Unit = {},
    viewModel: BodyPlannerViewModel? = null,
) {
    val context = LocalContext.current
    val vm: BodyPlannerViewModel = viewModel ?: run {
        val app = context.applicationContext as? AroApplication
        if (app != null) {
            viewModel(factory = BodyPlannerViewModel.factory(app.repository))
        } else {
            val db = AroDatabase.get(context)
            viewModel(factory = BodyPlannerViewModel.factory(RoomAroRepository(db)))
        }
    }

    val state by vm.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(initialTab.coerceIn(0, 1)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (selectedTab == 0) "Body, Weight & Energy" else "Aro Clinical Audit",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        )
                        Text(
                            text = if (selectedTab == 0) "Mifflin-St Jeor, BMI trend & weigh-ins" else "Evidence-Based Electrolyte Audit",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Body & Weight") },
                    icon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Aro Audit") },
                    icon = { Icon(Icons.Default.HealthAndSafety, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
            }

            when (val s = state) {
                is BodyPlannerUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is BodyPlannerUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Error loading planner: ${s.throwable.localizedMessage}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                is BodyPlannerUiState.Success -> {
                    if (selectedTab == 0) {
                        BodyPlannerTabContent(
                            profile = s.profile,
                            energyPlan = s.energyPlan,
                            bmiResult = s.bmiResult,
                            weightHistory = s.weightHistory,
                            onUpdateProfile = { vm.updateProfile(it) },
                            onLogWeight = { weight -> vm.logWeight(weight) },
                        )
                    } else {
                        ClinicalAuditTabContent(
                            assessment = s.clinicalAssessment,
                            activeProtocol = s.activeProtocol,
                            selectedDate = s.selectedDate,
                            formattedDate = s.formattedDate,
                            isToday = s.isToday,
                            onSelectProtocol = { vm.selectProtocol(it) },
                            onSelectDate = { vm.selectDate(it) },
                            onToday = { vm.today() },
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// TAB 0: BODY & ENERGY PLANNER CONTENT
// =============================================================================

@Composable
private fun BodyPlannerTabContent(
    profile: UserProfile,
    energyPlan: EnergyPlan,
    bmiResult: BmiResult?,
    weightHistory: List<Pair<LocalDate, Double>>,
    onUpdateProfile: (UserProfile) -> Unit,
    onLogWeight: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember(profile.name) { mutableStateOf(profile.name) }
    var sex by remember(profile.sex) { mutableStateOf(profile.sex) }
    var ageText by remember(profile.ageYears) { mutableStateOf(profile.ageYears.toString()) }
    var heightText by remember(profile.heightCm) { mutableStateOf(profile.heightCm.toString()) }
    var weightText by remember(profile.weightKg) { mutableStateOf(profile.weightKg.toString()) }
    var targetWeightText by remember(profile.targetWeightKg) { mutableStateOf(profile.targetWeightKg.toString()) }
    var activityLevel by remember(profile.activityLevel) { mutableStateOf(profile.activityLevel) }
    var goal by remember(profile.goal) { mutableStateOf(profile.goal) }
    var macroPreset by remember(profile.macroPreset) { mutableStateOf(profile.macroPreset) }

    var showWeighInDialog by remember { mutableStateOf(false) }

    fun commitProfile() {
        val parsedAge = ageText.toIntOrNull() ?: profile.ageYears
        val parsedHeight = heightText.toDoubleOrNull() ?: profile.heightCm
        val parsedWeight = weightText.toDoubleOrNull() ?: profile.weightKg
        val parsedTarget = targetWeightText.toDoubleOrNull() ?: profile.targetWeightKg

        val updated = profile.copy(
            name = name.ifBlank { "User" },
            sex = sex,
            ageYears = parsedAge,
            heightCm = parsedHeight,
            weightKg = parsedWeight,
            targetWeightKg = parsedTarget,
            activityLevel = activityLevel,
            goal = goal,
            macroPreset = macroPreset,
        )
        onUpdateProfile(updated)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 1. Anthropometrics Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "ANTHROPOMETRICS",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    letterSpacing = 1.2.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "Biological baseline for Mifflin-St Jeor BMR",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { showWeighInDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Scale,
                                contentDescription = "Log weigh-in",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    if (profile.validationErrors().isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Profile validation warning:",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                                profile.validationErrors().forEach { err ->
                                    Text(
                                        text = "• $err",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                }
                            }
                        }
                    }

                    // Sex Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Sex.entries.forEach { s ->
                            FilterChip(
                                selected = sex == s,
                                onClick = {
                                    sex = s
                                    commitProfile()
                                },
                                label = { Text(s.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                            )
                        }
                    }

                    // Age & Height
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedTextField(
                            value = ageText,
                            onValueChange = {
                                ageText = it
                                it.toIntOrNull()?.let { a ->
                                    if (a in UserProfile.AGE_RANGE) commitProfile()
                                }
                            },
                            label = { Text("Age (yrs)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )

                        OutlinedTextField(
                            value = heightText,
                            onValueChange = {
                                heightText = it
                                it.toDoubleOrNull()?.let { h ->
                                    if (h in UserProfile.HEIGHT_RANGE_CM) commitProfile()
                                }
                            },
                            label = { Text("Height (cm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                    }

                    // Current Weight & Target Weight
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedTextField(
                            value = weightText,
                            onValueChange = {
                                weightText = it
                                it.toDoubleOrNull()?.let { w ->
                                    if (w in UserProfile.WEIGHT_RANGE_KG) commitProfile()
                                }
                            },
                            label = { Text("Weight (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )

                        OutlinedTextField(
                            value = targetWeightText,
                            onValueChange = {
                                targetWeightText = it
                                it.toDoubleOrNull()?.let { tw ->
                                    if (tw in UserProfile.WEIGHT_RANGE_KG) commitProfile()
                                }
                            },
                            label = { Text("Target (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                    }

                    val errors = profile.validationErrors()
                    if (errors.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errors.first(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Weight, BMI and trend graph
        item {
            WeightBmiTrendCard(
                profile = profile,
                bmiResult = bmiResult,
                weightHistory = weightHistory,
                onOpenWeighIn = { showWeighInDialog = true },
            )
        }

        // 3. Physical Activity Level (PAL)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "PHYSICAL ACTIVITY LEVEL (PAL)",
                        style = MaterialTheme.typography.labelMedium.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Multipliers applied to BMR to calculate Total Energy Expenditure (TDEE)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    ActivityLevel.entries.forEach { level ->
                        val isSelected = activityLevel == level
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    activityLevel = level
                                    commitProfile()
                                },
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = level.label,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        ),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                                ) {
                                    Text(
                                        text = "${level.tdeeMultiplier}x",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Metabolic Goal
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "METABOLIC STRATEGY & GOAL",
                        style = MaterialTheme.typography.labelMedium.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Daily energy offset applied to TDEE (floored at 1,200 kcal safety limit)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Goal.entries.forEach { g ->
                        val isSelected = goal == g
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    goal = g
                                    commitProfile()
                                },
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = g.label,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        ),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    )
                                    val weeklyStr = if (g.calorieAdjustment == 0) "Maintenance" else {
                                        String.format("%.2f kg/wk", g.weeklyChangeKg)
                                    }
                                    Text(
                                        text = "Projected change: $weeklyStr",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                                ) {
                                    val offsetSign = if (g.calorieAdjustment > 0) "+" else ""
                                    Text(
                                        text = "$offsetSign${g.calorieAdjustment} kcal",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Macro Preset
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "MACRONUTRIENT DISTRIBUTION",
                        style = MaterialTheme.typography.labelMedium.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Atwater ratio (4 kcal/g protein & carbs, 9 kcal/g fat)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    MacroPreset.entries.forEach { preset ->
                        val isSelected = macroPreset == preset
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    macroPreset = preset
                                    commitProfile()
                                },
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = preset.label,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        ),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }

                                MacroDistributionBar(
                                    proteinRatio = preset.proteinRatio,
                                    carbRatio = preset.carbohydrateRatio,
                                    fatRatio = preset.fatRatio,
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. Energy Engine Calculations
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "ENERGY ENGINE SUMMARY",
                            style = MaterialTheme.typography.labelMedium.copy(
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    // BMR, TDEE, Calorie Goal row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        EnergyMetricPill(
                            label = "BMR",
                            value = "${energyPlan.bmr}",
                            unit = "kcal",
                            subtitle = "Mifflin-St Jeor",
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        EnergyMetricPill(
                            label = "TDEE",
                            value = "${energyPlan.tdee}",
                            unit = "kcal",
                            subtitle = "Expenditure",
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        EnergyMetricPill(
                            label = "TARGET",
                            value = "${energyPlan.targetCalories}",
                            unit = "kcal",
                            subtitle = "Daily Goal",
                            highlight = true,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    if (energyPlan.hitSafetyFloor) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Target floored at 1,200 kcal/day to safeguard metabolic and endocrine health.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Macro Targets Table
                    Text(
                        text = "Daily Macronutrient Gram Targets",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        MacroGramBadge(
                            label = "Protein",
                            grams = energyPlan.macros.proteinG,
                            color = AroTheme.macroColors.protein,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        MacroGramBadge(
                            label = "Carbs",
                            grams = energyPlan.macros.carbsG,
                            color = AroTheme.macroColors.carbs,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        MacroGramBadge(
                            label = "Fat",
                            grams = energyPlan.macros.fatG,
                            color = AroTheme.macroColors.fat,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        MacroGramBadge(
                            label = "Fiber",
                            grams = energyPlan.macros.fiberG,
                            color = AroTheme.macroColors.fiber,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showWeighInDialog) {
        var newWeightInput by remember { mutableStateOf(profile.weightKg.toString()) }
        AlertDialog(
            onDismissRequest = { showWeighInDialog = false },
            title = { Text("Record Weigh-in") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter today's scale weight (kg). This updates your active profile and logs a permanent weigh-in point.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = newWeightInput,
                        onValueChange = { newWeightInput = it },
                        label = { Text("Weight (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = newWeightInput.toDoubleOrNull()
                        if (parsed != null && parsed in UserProfile.WEIGHT_RANGE_KG) {
                            onLogWeight(parsed)
                            weightText = parsed.toString()
                            showWeighInDialog = false
                        }
                    },
                ) {
                    Text("Save Weigh-in")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWeighInDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

// =============================================================================
// TAB 1: DR. ARO CLINICAL AUDIT CONTENT
// =============================================================================

@Composable
private fun ClinicalAuditTabContent(
    assessment: ClinicalAssessment,
    activeProtocol: ClinicalProtocol,
    selectedDate: LocalDate,
    formattedDate: String,
    isToday: Boolean,
    onSelectProtocol: (String) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Date Selector Bar
        item {
            AroDatePickerBar(
                selectedDate = selectedDate,
                formattedDate = formattedDate,
                isToday = isToday,
                onSelectDate = onSelectDate,
                onToday = onToday,
                label = "AUDIT DATE",
            )
        }

        // Active Clinical Protocol Selector
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "CLINICAL PROTOCOL",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    letterSpacing = 1.2.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "Evidence-based cardiovascular & metabolic guidelines",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        ) {
                            Text(
                                text = activeProtocol.badge,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }

                    // Protocols list
                    ClinicalCatalog.all.forEach { proto ->
                        val isSelected = proto.id == activeProtocol.id
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectProtocol(proto.id) },
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = proto.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        ),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    )
                                    if (isSelected) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary,
                                        ) {
                                            Text(
                                                text = "ACTIVE",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp,
                                                ),
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = proto.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                )

                                Text(
                                    text = "Citation: ${proto.citation}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                )

                                // Protocol Targets Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    TargetChip(label = "Na ≤${proto.maxSodiumMg}mg", isSelected = isSelected)
                                    val kLabel = if (proto.potassiumIsCeiling) "K ≤${proto.targetPotassiumMg}mg (Ceiling)" else "K ≥${proto.targetPotassiumMg}mg"
                                    TargetChip(label = kLabel, isSelected = isSelected)
                                    TargetChip(label = "Fiber ≥${proto.minFiberPer1000KcalG}g/1000kcal", isSelected = isSelected)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Visual Molar Na:K Spectrum Meter
        item {
            MolarNaKSpectrumCard(ratio = assessment.naKRatio)
        }

        // Electrolytes & Nutrient Clinical Verdicts Grid
        item {
            NutrientVerdictsCard(assessment = assessment)
        }

        // Dr. Aro Clinical Findings & Citations
        item {
            ClinicalFindingsCard(assessment = assessment)
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// =============================================================================
// MOLAR NA:K SPECTRUM METER
// =============================================================================

@Composable
private fun MolarNaKSpectrumCard(
    ratio: NaKRatio,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "MOLAR NA:K RATIO METER",
                        style = MaterialTheme.typography.labelMedium.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Cardiovascular endothelial tone & stroke risk indicator",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                val statusBadgeColor = when (ratio.status) {
                    RatioStatus.OPTIMAL -> AroClinicalOptimal
                    RatioStatus.MODERATE -> AroClinicalModerate
                    RatioStatus.HIGH_RISK -> AroClinicalHighRisk
                    RatioStatus.UNDETERMINED -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusBadgeColor.copy(alpha = 0.15f),
                ) {
                    Text(
                        text = ratio.status.shortLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = statusBadgeColor,
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            // Ratio numbers strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (ratio.isDetermined) "${ratio.molarRatio}" else "—",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Molar Ratio (Clinically Valid)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (ratio.isDetermined) "${ratio.massRatio}" else "—",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Mass Ratio (mg/mg)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Spectrum Bar Canvas
            NaKSpectrumMeterBar(
                position = ratio.spectrumPosition,
                isDetermined = ratio.isDetermined,
            )

            // Spectrum legend ticks
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "0.0\nOptimal",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = AroClinicalOptimal,
                    textAlign = TextAlign.Start,
                )
                Text(
                    text = "1.0\nTarget Max",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = AroClinicalModerate,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "2.0\nAtherogenic",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = AroClinicalHighRisk,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "3.0+\nSevere Risk",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = AroClinicalHighRisk,
                    textAlign = TextAlign.End,
                )
            }

            // Clinical Insight Explanation
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp).padding(top = 2.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Sodium (22.99 g/mol) and potassium (39.10 g/mol) have different molar weights: molar = mass × 1.7007. Cardiovascular clinical trials (DASH, PURE, INTERSALT) demonstrate vascular compliance and stroke risk correlate with the molar ratio, with < 1.0 being cardioprotective.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun NaKSpectrumMeterBar(
    position: Float,
    isDetermined: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(16.dp)) {
            val trackHeight = size.height
            val trackWidth = size.width

            val gradient = Brush.horizontalGradient(
                colors = listOf(
                    AroClinicalOptimal,
                    AroClinicalOptimal,
                    AroClinicalModerate,
                    AroClinicalHighRisk,
                    Color(0xFFBE123C),
                ),
            )

            // Background spectrum track
            drawRoundRect(
                brush = gradient,
                size = Size(trackWidth, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2, trackHeight / 2),
            )

            // Tick at 1.0 (1.0 / 3.0 = 0.333f)
            val tick1X = trackWidth * (1.0f / 3.0f)
            drawLine(
                color = Color.White.copy(alpha = 0.6f),
                start = Offset(tick1X, 0f),
                end = Offset(tick1X, trackHeight),
                strokeWidth = 2f,
            )

            // Tick at 2.0 (2.0 / 3.0 = 0.666f)
            val tick2X = trackWidth * (2.0f / 3.0f)
            drawLine(
                color = Color.White.copy(alpha = 0.6f),
                start = Offset(tick2X, 0f),
                end = Offset(tick2X, trackHeight),
                strokeWidth = 2f,
            )

            // Needle indicator
            if (isDetermined) {
                val needleX = (trackWidth * position).coerceIn(4f, trackWidth - 4f)
                drawCircle(
                    color = Color.White,
                    radius = 10f,
                    center = Offset(needleX, trackHeight / 2),
                )
                drawCircle(
                    color = Color.Black,
                    radius = 5f,
                    center = Offset(needleX, trackHeight / 2),
                )
            }
        }
    }
}

// =============================================================================
// NUTRIENT & ELECTROLYTE VERDICTS CARD
// =============================================================================

@Composable
private fun NutrientVerdictsCard(
    assessment: ClinicalAssessment,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "ELECTROLYTE & NUTRIENT VERDICTS",
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.primary,
            )

            assessment.verdicts.forEach { v ->
                NutrientVerdictRow(verdict = v)
            }
        }
    }
}

@Composable
private fun NutrientVerdictRow(
    verdict: NutrientVerdict,
    modifier: Modifier = Modifier,
) {
    val (statusText, statusColor) = when (verdict.status) {
        AuditStatus.ON_TARGET -> "Within boundary" to AroClinicalOptimal
        AuditStatus.BELOW_TARGET -> "Below target" to AroClinicalModerate
        AuditStatus.EXCEEDS_LIMIT -> "Limit exceeded" to AroClinicalHighRisk
        AuditStatus.ABSENT -> "Not logged" to MaterialTheme.colorScheme.onSurfaceVariant
        AuditStatus.UNREPORTED -> "Not tracked" to MaterialTheme.colorScheme.onSurfaceVariant
        AuditStatus.NOT_ENOUGH_DATA -> "Low intake" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = verdict.key.label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val targetText = if (verdict.target != null) {
                    val op = if (verdict.isUpperLimit) "≤" else "≥"
                    val actualRounded = verdict.actual?.roundToInt() ?: 0
                    val targetRounded = verdict.target.roundToInt()
                    "$actualRounded $op $targetRounded ${verdict.unitLabel}"
                } else {
                    "No protocol target"
                }
                Text(
                    text = targetText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = statusColor.copy(alpha = 0.15f),
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

// =============================================================================
// DR. ARO CLINICAL FINDINGS CARD
// =============================================================================

@Composable
private fun ClinicalFindingsCard(
    assessment: ClinicalAssessment,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "DR. ARO CLINICAL FINDINGS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = "${assessment.findings.size} Notes",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }

            if (assessment.findings.isEmpty()) {
                Text(
                    text = "No clinical warnings. Daily nutrition adheres to the active protocol boundaries.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                assessment.findings.forEach { finding ->
                    ClinicalFindingItem(finding = finding)
                }
            }
        }
    }
}

@Composable
private fun ClinicalFindingItem(
    finding: ClinicalFinding,
    modifier: Modifier = Modifier,
) {
    val (badgeText, badgeColor) = when (finding.severity) {
        FindingSeverity.CRITICAL -> "CARDIOVASCULAR ALERT" to AroClinicalHighRisk
        FindingSeverity.WARNING -> "WARNING" to AroClinicalModerate
        FindingSeverity.ADVISORY -> "DEFICIT" to AroClinicalCeiling
        FindingSeverity.INFO -> "CLINICAL NOTE" to MaterialTheme.colorScheme.primary
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor.copy(alpha = 0.15f),
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            fontSize = 10.sp,
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                Text(
                    text = finding.code.name.replace('_', ' '),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = finding.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

// =============================================================================
// REUSABLE HELPER UI COMPONENTS
// =============================================================================

@Composable
private fun TargetChip(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = modifier,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun EnergyMetricPill(
    label: String,
    value: String,
    unit: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (highlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                ),
                color = if (highlight) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (highlight) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = if (highlight) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = if (highlight) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun MacroGramBadge(
    label: String,
    grams: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                ),
                color = color,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${grams}g",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun MacroDistributionBar(
    proteinRatio: Double,
    carbRatio: Double,
    fatRatio: Double,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
    ) {
        Box(
            modifier = Modifier
                .weight(proteinRatio.toFloat())
                .fillMaxSize()
                .background(AroTheme.macroColors.protein),
        )
        Box(
            modifier = Modifier
                .weight(carbRatio.toFloat())
                .fillMaxSize()
                .background(AroTheme.macroColors.carbs),
        )
        Box(
            modifier = Modifier
                .weight(fatRatio.toFloat())
                .fillMaxSize()
                .background(AroTheme.macroColors.fat),
        )
    }
}
