package com.drnitz.aro.ui.food

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.drnitz.aro.data.library.OfflineFoodLibrary
import com.drnitz.aro.data.network.OpenFoodFactsClient
import com.drnitz.aro.domain.calculation.EnergyPlan
import com.drnitz.aro.domain.model.CuratedFoodItem
import com.drnitz.aro.domain.model.DiaryDay
import com.drnitz.aro.domain.model.EnergyBalance
import com.drnitz.aro.domain.model.FoodLogEntry
import com.drnitz.aro.domain.model.MealSlot
import com.drnitz.aro.domain.model.NutrientTotals
import com.drnitz.aro.theme.AroTheme
import com.drnitz.aro.ui.bodymap.BodySoundHaptics
import com.drnitz.aro.ui.components.AroDialogSurface
import com.drnitz.aro.ui.components.AroLoggingDateChip
import com.drnitz.aro.ui.components.AroLoggingDateLabel
import com.drnitz.aro.ui.components.BarcodeScannerView
import com.drnitz.aro.ui.components.ConcentricMacroRings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToInt

private enum class FoodSource { OFFLINE, ONLINE }

private enum class FoodFilter(val label: String) {
    ALL("All"),
    CURATED("Curated"),
    FAVORITES("Favorites"),
    RECENT("Recent"),
}

/**
 * Dedicated Food & Nutrition tab.
 *
 * Mirrors the web food database: image cards with Nutri-Score/NOVA badges, instant
 * search, favourite/recents filters and a barcode shortcut. Offline whole foods remain
 * the default source; Open Food Facts is an explicit opt-in so the app never requires a
 * network to log a meal.
 */
@Composable
fun FoodDatabaseScreen(
    foodLibrary: OfflineFoodLibrary,
    favoriteFoods: List<CuratedFoodItem>,
    recentFoods: List<CuratedFoodItem>,
    selectedDate: LocalDate,
    formattedDate: String,
    isToday: Boolean,
    onSelectDate: (LocalDate) -> Unit,
    onToday: () -> Unit,
    onFoodLogged: (FoodLogEntry, CuratedFoodItem) -> Unit,
    onToggleFavorite: (CuratedFoodItem) -> Unit,
    modifier: Modifier = Modifier,
    openFoodFactsClient: OpenFoodFactsClient? = null,
    isPlanning: Boolean = false,
    energyPlan: EnergyPlan? = null,
    energyBalance: EnergyBalance? = null,
    dayTotals: NutrientTotals? = null,
    diaryDay: DiaryDay? = null,
) {
    val density = LocalDensity.current.density
    var isSoundMuted by remember { mutableStateOf(false) }
    var isMealDistributionShowing by remember { mutableStateOf(false) }

    val suggestedMeal = remember {
        when (LocalTime.now().hour) {
            in 5..10 -> MealSlot.BREAKFAST
            in 11..15 -> MealSlot.LUNCH
            in 16..21 -> MealSlot.DINNER
            else -> MealSlot.SNACKS
        }
    }
    var activeMealSlot by remember { mutableStateOf(suggestedMeal) }

    // Smooth 3D Card Rotation between Macro Fuel Gauge (0°) and Meal Distribution (180°)
    val rotation by animateFloatAsState(
        targetValue = if (!isMealDistributionShowing) 0f else 180f,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "food_3d_card_flip",
    )
    val isFrontShowing = rotation <= 90f

    val effectiveTargetCalories = energyPlan?.targetCalories ?: 2000
    val effectiveTargetProtein = energyPlan?.macros?.proteinG?.toDouble() ?: 150.0
    val effectiveTargetCarbs = energyPlan?.macros?.carbsG?.toDouble() ?: 200.0
    val effectiveTargetFat = energyPlan?.macros?.fatG?.toDouble() ?: 65.0

    val effectiveCaloriesEaten = energyBalance?.caloriesEaten ?: dayTotals?.calories?.roundToInt() ?: 0
    val effectiveCaloriesBurned = energyBalance?.caloriesBurned ?: 0
    val effectiveProteinEaten = dayTotals?.proteinG ?: 0.0
    val effectiveCarbsEaten = dayTotals?.carbsG ?: 0.0
    val effectiveFatEaten = dayTotals?.fatG ?: 0.0

    var query by remember { mutableStateOf("") }
    var source by remember { mutableStateOf(FoodSource.OFFLINE) }
    var filter by remember { mutableStateOf(FoodFilter.ALL) }
    var selectedCategory by remember { mutableStateOf("All") }
    var categories by remember { mutableStateOf(listOf("All")) }

    var offlineResults by remember { mutableStateOf<List<CuratedFoodItem>>(emptyList()) }
    var allCuratedFoods by remember { mutableStateOf<List<CuratedFoodItem>>(emptyList()) }
    var isOfflineLoading by remember { mutableStateOf(true) }
    var onlineResults by remember { mutableStateOf<List<CuratedFoodItem>>(emptyList()) }
    var isOnlineLoading by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }

    var configuringFood by remember { mutableStateOf<CuratedFoodItem?>(null) }
    var showScanner by remember { mutableStateOf(false) }
    var drawerExpanded by remember { mutableStateOf(false) }
    var drawerFullScreen by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val curatedById = remember(allCuratedFoods) { allCuratedFoods.associateBy { it.id } }
    val enrichedFavorites = remember(favoriteFoods, curatedById) {
        favoriteFoods.map { curatedById[it.id] ?: it }
    }
    val enrichedRecents = remember(recentFoods, curatedById) {
        recentFoods.map { curatedById[it.id] ?: it }
    }
    val favoriteIds = remember(enrichedFavorites) { enrichedFavorites.map { it.id }.toSet() }

    LaunchedEffect(Unit) {
        allCuratedFoods = foodLibrary.getCuratedFoods()
        categories = listOf("All") + foodLibrary.getCategories()
    }

    LaunchedEffect(query, selectedCategory) {
        isOfflineLoading = true
        val category = selectedCategory.takeIf { it != "All" }
        offlineResults = foodLibrary.search(query = query, category = category)
        isOfflineLoading = false
    }

    LaunchedEffect(query, source) {
        if (source != FoodSource.ONLINE) {
            isOnlineLoading = false
            searchError = null
            return@LaunchedEffect
        }
        val clean = query.trim()
        if (clean.length < 2 || openFoodFactsClient == null) {
            isOnlineLoading = false
            onlineResults = emptyList()
            searchError = null
            return@LaunchedEffect
        }
        isOnlineLoading = true
        searchError = null
        delay(450)
        try {
            val results = openFoodFactsClient.searchProducts(clean)
            onlineResults = results.distinctBy { it.id }
            if (results.isEmpty()) searchError = "No Open Food Facts products matched \"$clean\"."
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            searchError = "Open Food Facts search failed: ${e.message}"
        } finally {
            isOnlineLoading = false
        }
    }

    val displayResults: List<CuratedFoodItem> = when {
        source == FoodSource.ONLINE -> onlineResults
        filter == FoodFilter.ALL -> (offlineResults + enrichedFavorites + enrichedRecents).distinctBy { it.id }
        filter == FoodFilter.CURATED -> offlineResults
        filter == FoodFilter.FAVORITES -> enrichedFavorites.filter { it.matchesQuery(query) }
        else -> enrichedRecents.filter { it.matchesQuery(query) }
    }

    if (showScanner) {
        Dialog(
            onDismissRequest = { showScanner = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            BarcodeScannerView(
                onBarcodeDetected = { barcode ->
                    scope.launch {
                        searchError = null
                        val offline = foodLibrary.findByBarcode(barcode)
                        if (offline != null) {
                            configuringFood = offline
                            showScanner = false
                            return@launch
                        }
                        val online = openFoodFactsClient?.getProductByBarcode(barcode)
                        if (online != null) {
                            configuringFood = online
                            showScanner = false
                            return@launch
                        }
                        searchError = "No product found for barcode $barcode."
                        showScanner = false
                    }
                },
                onClose = { showScanner = false },
            )
        }
    }

    val currentMealCals = diaryDay?.totalsFor(activeMealSlot)?.calories?.roundToInt() ?: 0

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Layer 1: Top Status Row (Left-aligned, leaving right side open for side dock)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 68.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AroLoggingDateChip(
                    selectedDate = selectedDate,
                    formattedDate = formattedDate,
                    isToday = isToday,
                    onSelectDate = onSelectDate,
                    onToday = onToday,
                    labelPrefix = if (isPlanning) "Plan for" else "Log for",
                )

                // Active Meal Slot Pill
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    shadowElevation = 2.dp,
                    modifier = Modifier.clickable {
                        val nextSlot = when (activeMealSlot) {
                            MealSlot.BREAKFAST -> MealSlot.LUNCH
                            MealSlot.LUNCH -> MealSlot.DINNER
                            MealSlot.DINNER -> MealSlot.SNACKS
                            MealSlot.SNACKS -> MealSlot.BREAKFAST
                        }
                        activeMealSlot = nextSlot
                        BodySoundHaptics.playClickSound(isSoundMuted)
                    },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        val mealEmoji = when (activeMealSlot) {
                            MealSlot.BREAKFAST -> "🍳"
                            MealSlot.LUNCH -> "🥗"
                            MealSlot.DINNER -> "🍲"
                            MealSlot.SNACKS -> "🍎"
                        }
                        Text(
                            text = "$mealEmoji ${activeMealSlot.label} • ${currentMealCals} kcal",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            // Layer 0: Center-Stage Visualizer Card with 3D Flip
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(184.dp)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .graphicsLayer {
                        rotationY = if (isFrontShowing) rotation else rotation - 180f
                        cameraDistance = 14f * density
                    },
            ) {
                if (isFrontShowing) {
                    // FRONT: Concentric Macro Rings + Macro Fuel Target Bars
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            modifier = Modifier.size(136.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            ConcentricMacroRings(
                                caloriesEaten = effectiveCaloriesEaten,
                                caloriesBurned = effectiveCaloriesBurned,
                                targetCalories = effectiveTargetCalories,
                                proteinEatenG = effectiveProteinEaten,
                                targetProteinG = effectiveTargetProtein,
                                carbsEatenG = effectiveCarbsEaten,
                                targetCarbsG = effectiveTargetCarbs,
                                fatEatenG = effectiveFatEaten,
                                targetFatG = effectiveTargetFat,
                                size = 136.dp,
                                strokeWidth = 7.dp,
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "DAILY FUEL TARGETS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.5.sp,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            MacroTargetProgressRow(
                                label = "Protein",
                                eatenG = effectiveProteinEaten,
                                targetG = effectiveTargetProtein,
                                color = AroTheme.macroColors.protein,
                            )

                            MacroTargetProgressRow(
                                label = "Carbs",
                                eatenG = effectiveCarbsEaten,
                                targetG = effectiveTargetCarbs,
                                color = AroTheme.macroColors.carbs,
                            )

                            MacroTargetProgressRow(
                                label = "Fat",
                                eatenG = effectiveFatEaten,
                                targetG = effectiveTargetFat,
                                color = AroTheme.macroColors.fat,
                            )
                        }
                    }
                } else {
                    // BACK: Meal Distribution (2x2 Grid of Breakfast, Lunch, Dinner, Snacks)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "MEAL DISTRIBUTION",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.5.sp,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "Tap meal to set active target",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            MealSlotCard(
                                slot = MealSlot.BREAKFAST,
                                emoji = "🍳",
                                isSelected = activeMealSlot == MealSlot.BREAKFAST,
                                cals = diaryDay?.totalsFor(MealSlot.BREAKFAST)?.calories?.roundToInt() ?: 0,
                                count = diaryDay?.meals?.get(MealSlot.BREAKFAST)?.size ?: 0,
                                modifier = Modifier.weight(1f),
                                onSelect = {
                                    activeMealSlot = MealSlot.BREAKFAST
                                    drawerExpanded = true
                                    BodySoundHaptics.playClickSound(isSoundMuted)
                                },
                            )
                            MealSlotCard(
                                slot = MealSlot.LUNCH,
                                emoji = "🥗",
                                isSelected = activeMealSlot == MealSlot.LUNCH,
                                cals = diaryDay?.totalsFor(MealSlot.LUNCH)?.calories?.roundToInt() ?: 0,
                                count = diaryDay?.meals?.get(MealSlot.LUNCH)?.size ?: 0,
                                modifier = Modifier.weight(1f),
                                onSelect = {
                                    activeMealSlot = MealSlot.LUNCH
                                    drawerExpanded = true
                                    BodySoundHaptics.playClickSound(isSoundMuted)
                                },
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            MealSlotCard(
                                slot = MealSlot.DINNER,
                                emoji = "🍲",
                                isSelected = activeMealSlot == MealSlot.DINNER,
                                cals = diaryDay?.totalsFor(MealSlot.DINNER)?.calories?.roundToInt() ?: 0,
                                count = diaryDay?.meals?.get(MealSlot.DINNER)?.size ?: 0,
                                modifier = Modifier.weight(1f),
                                onSelect = {
                                    activeMealSlot = MealSlot.DINNER
                                    drawerExpanded = true
                                    BodySoundHaptics.playClickSound(isSoundMuted)
                                },
                            )
                            MealSlotCard(
                                slot = MealSlot.SNACKS,
                                emoji = "🍎",
                                isSelected = activeMealSlot == MealSlot.SNACKS,
                                cals = diaryDay?.totalsFor(MealSlot.SNACKS)?.calories?.roundToInt() ?: 0,
                                count = diaryDay?.meals?.get(MealSlot.SNACKS)?.size ?: 0,
                                modifier = Modifier.weight(1f),
                                onSelect = {
                                    activeMealSlot = MealSlot.SNACKS
                                    drawerExpanded = true
                                    BodySoundHaptics.playClickSound(isSoundMuted)
                                },
                            )
                        }
                    }
                }
            }

            // Active Meal Summary & Day Overview (Visible when drawer is peeked/collapsed)
            val currentMealEntries = diaryDay?.meals?.get(activeMealSlot).orEmpty()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Active Meal Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${activeMealSlot.label.uppercase()} LOG (${currentMealCals} kcal)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                ),
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable {
                                    drawerExpanded = true
                                    BodySoundHaptics.playClickSound(isSoundMuted)
                                },
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Add Food",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            }
                        }

                        if (currentMealEntries.isEmpty()) {
                            Text(
                                text = "Nothing logged for ${activeMealSlot.label} yet. Open the catalog drawer below or tap Add Food to search whole foods.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                currentMealEntries.forEach { entry ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = entry.name,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = "${entry.grams.roundToInt()}g • P:${entry.proteinG.roundToInt()}g C:${entry.nutrients.carbsG.roundToInt()}g F:${entry.nutrients.fatG.roundToInt()}g",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        Text(
                                            text = "${entry.calories.roundToInt()} kcal",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = AroTheme.macroColors.calories,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Daily Nutrition Budget Breakdown Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f),
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "DAILY ENERGY EQUATION",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            EquationStat("Target", "$effectiveTargetCalories", MaterialTheme.colorScheme.onSurface)
                            EquationStat("Consumed", "$effectiveCaloriesEaten", AroTheme.macroColors.calories)
                            EquationStat("Burned", "$effectiveCaloriesBurned", AroTheme.macroColors.protein)
                            val remaining = (effectiveTargetCalories - (effectiveCaloriesEaten - effectiveCaloriesBurned)).coerceAtLeast(0)
                            EquationStat("Remaining", "$remaining", MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(70.dp))
            }
        }

        // Layer 2: Embedded Vertical Side Dock (TopEnd)
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 10.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            shadowElevation = 4.dp,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp),
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp),
            ) {
                // 3D Flip (MACRO / MEALS)
                IconButton(
                    onClick = {
                        isMealDistributionShowing = !isMealDistributionShowing
                        BodySoundHaptics.playClickSound(isSoundMuted)
                    },
                    modifier = Modifier.size(36.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Cached,
                            contentDescription = "Flip nutrition card",
                            tint = AroTheme.macroColors.carbs,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = if (!isMealDistributionShowing) "MACRO" else "MEALS",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                // Barcode Scanner Launcher
                IconButton(
                    onClick = {
                        showScanner = true
                        BodySoundHaptics.playClickSound(isSoundMuted)
                    },
                    modifier = Modifier.size(36.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan barcode",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "SCAN",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                // Source Toggle (Offline Whole Foods / Open Food Facts)
                IconButton(
                    onClick = {
                        source = if (source == FoodSource.OFFLINE) FoodSource.ONLINE else FoodSource.OFFLINE
                        BodySoundHaptics.playClickSound(isSoundMuted)
                    },
                    modifier = Modifier.size(36.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (source == FoodSource.OFFLINE) Icons.Default.Restaurant else Icons.Default.Language,
                            contentDescription = "Toggle food source",
                            tint = if (source == FoodSource.ONLINE) AroTheme.macroColors.fat else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = if (source == FoodSource.OFFLINE) "WHOLE" else "ONLINE",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.width(22.dp).padding(vertical = 2.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                )

                // Audio Mute Toggle
                IconButton(
                    onClick = { isSoundMuted = !isSoundMuted },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = if (isSoundMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = if (isSoundMuted) "Unmute audio" else "Mute audio",
                        tint = if (isSoundMuted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        // Layer 3: Collapsed Bottom Peek Bar
        AnimatedVisibility(
            visible = !drawerExpanded,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Surface(
                onClick = {
                    drawerExpanded = true
                    BodySoundHaptics.playClickSound(isSoundMuted)
                },
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount < -10f) {
                                drawerExpanded = true
                                BodySoundHaptics.playClickSound(isSoundMuted)
                            }
                        }
                    },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = CircleShape,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Restaurant,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        Column {
                            Text(
                                text = if (selectedCategory != "All") "$selectedCategory Foods" else if (source == FoodSource.ONLINE) "Open Food Facts" else "Whole Foods Catalog",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "Log to ${activeMealSlot.label} • ${displayResults.size} items • Tap to browse",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Expand food catalog drawer",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        // Layer 4: Expanded Bottom Sliding Food Drawer (Phase 3 & 4 & 5)
        AnimatedVisibility(
            visible = drawerExpanded,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            val drawerHeightFraction = if (drawerFullScreen) 0.92f else 0.58f

            Card(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(drawerHeightFraction),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp)
                        .padding(top = 8.dp),
                ) {
                    // Drag Handle & Vertical Swipe Physics
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(drawerFullScreen) {
                                detectVerticalDragGestures { _, dragAmount ->
                                    if (dragAmount < -15f && !drawerFullScreen) {
                                        drawerFullScreen = true
                                    } else if (dragAmount > 15f) {
                                        if (drawerFullScreen) {
                                            drawerFullScreen = false
                                        } else {
                                            drawerExpanded = false
                                        }
                                    }
                                }
                            }
                            .padding(vertical = 4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .width(44.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    // Drawer Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = if (selectedCategory != "All") "$selectedCategory Foods" else "Food Catalog",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text(
                                    text = "${displayResults.size}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                            if (selectedCategory != "All") {
                                TextButton(
                                    onClick = { selectedCategory = "All" },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                ) {
                                    Text("Clear", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Fullscreen / Partial height toggle
                            IconButton(
                                onClick = {
                                    drawerFullScreen = !drawerFullScreen
                                    BodySoundHaptics.playClickSound(isSoundMuted)
                                },
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    imageVector = if (drawerFullScreen) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                    contentDescription = if (drawerFullScreen) "Collapse height" else "Expand full height",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            // Close drawer
                            IconButton(
                                onClick = {
                                    drawerExpanded = false
                                    BodySoundHaptics.playClickSound(isSoundMuted)
                                },
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close food drawer",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    // Phase 4: Compact 40dp Search Bar Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp),
                                )
                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                    if (query.isEmpty()) {
                                        Text(
                                            text = if (source == FoodSource.ONLINE) "Search Open Food Facts..." else "Search 40+ whole foods...",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    BasicTextField(
                                        value = query,
                                        onValueChange = { query = it },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 13.sp,
                                        ),
                                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                                if (query.isNotEmpty()) {
                                    IconButton(
                                        onClick = { query = "" },
                                        modifier = Modifier.size(24.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear search",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { showScanner = true },
                                    modifier = Modifier.size(28.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "Scan barcode",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }

                    // Streamlined Category Filter & Source Row
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        item(key = "source_chip") {
                            FilterChip(
                                selected = source == FoodSource.ONLINE,
                                onClick = {
                                    source = if (source == FoodSource.OFFLINE) FoodSource.ONLINE else FoodSource.OFFLINE
                                },
                                label = {
                                    Text(
                                        if (source == FoodSource.ONLINE) "Online OFF" else "Offline Foods",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                            )
                        }

                        if (source == FoodSource.OFFLINE) {
                            items(FoodFilter.entries.toList(), key = { it.name }) { option ->
                                FilterChip(
                                    selected = filter == option,
                                    onClick = { filter = option },
                                    leadingIcon = if (option == FoodFilter.FAVORITES) {
                                        {
                                            Icon(
                                                imageVector = if (filter == option) Icons.Default.Star else Icons.Default.StarBorder,
                                                contentDescription = null,
                                                modifier = Modifier.size(13.dp),
                                            )
                                        }
                                    } else null,
                                    label = { Text(option.label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)) },
                                    shape = RoundedCornerShape(8.dp),
                                )
                            }

                            if (filter == FoodFilter.ALL || filter == FoodFilter.CURATED) {
                                items(categories, key = { "cat_$it" }) { category ->
                                    FilterChip(
                                        selected = selectedCategory == category,
                                        onClick = { selectedCategory = category },
                                        label = { Text(category, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)) },
                                        shape = RoundedCornerShape(8.dp),
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // Phase 5: Modernized Food Catalog List with Micro-Bars & Quick Log
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        when {
                            source == FoodSource.ONLINE && isOnlineLoading -> {
                                item(key = "online_loading") {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                            Spacer(Modifier.height(8.dp))
                                            Text("Searching Open Food Facts...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                            source == FoodSource.OFFLINE && isOfflineLoading && displayResults.isEmpty() -> {
                                item(key = "offline_loading") {
                                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                    }
                                }
                            }
                            (source == FoodSource.ONLINE && query.trim().length < 2) ||
                                (source == FoodSource.OFFLINE && filter == FoodFilter.FAVORITES && favoriteFoods.isEmpty()) ||
                                (source == FoodSource.OFFLINE && filter == FoodFilter.RECENT && recentFoods.isEmpty()) -> {
                                item(key = "empty_prompt") {
                                    EmptyFoodState(
                                        title = when {
                                            filter == FoodFilter.FAVORITES -> "No favorites yet"
                                            filter == FoodFilter.RECENT -> "Nothing logged recently"
                                            else -> "Search Open Food Facts"
                                        },
                                        subtitle = when {
                                            filter == FoodFilter.FAVORITES -> "Tap the star on any food to keep it one tap away."
                                            filter == FoodFilter.RECENT -> "Foods you log will appear here automatically."
                                            else -> "Type at least two characters or scan a barcode."
                                        },
                                    )
                                }
                            }
                            searchError != null && displayResults.isEmpty() -> {
                                item(key = "empty_error") {
                                    EmptyFoodState(
                                        title = "No results",
                                        subtitle = searchError!!,
                                        actionLabel = if (source == FoodSource.ONLINE) null else "Search Open Food Facts",
                                        onAction = { source = FoodSource.ONLINE },
                                    )
                                }
                            }
                            displayResults.isEmpty() -> {
                                item(key = "empty_not_found") {
                                    EmptyFoodState(
                                        title = if (query.isBlank()) "No foods available" else "No foods match \"$query\"",
                                        subtitle = if (source == FoodSource.OFFLINE) "The curated library is bundled offline. Try Open Food Facts for branded items." else "Try a different spelling or scan the barcode.",
                                        actionLabel = if (source == FoodSource.OFFLINE) "Search Open Food Facts" else null,
                                        onAction = { source = FoodSource.ONLINE },
                                    )
                                }
                            }
                            else -> {
                                items(displayResults, key = { it.id }) { food ->
                                    FoodDatabaseCard(
                                        food = food,
                                        isFavorite = food.id in favoriteIds,
                                        activeMealSlot = activeMealSlot,
                                        onOpen = { configuringFood = food },
                                        onQuickLog = {
                                            val defaultGrams = food.commonServing?.grams ?: 100.0
                                            val entry = food.toFoodLogEntry(meal = activeMealSlot, grams = defaultGrams)
                                            onFoodLogged(entry, food)
                                            BodySoundHaptics.playClickSound(isSoundMuted)
                                        },
                                        onToggleFavorite = { onToggleFavorite(food) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    configuringFood?.let { food ->
        PortionPickerDialog(
            food = food,
            targetDateLabel = formattedDate,
            targetDateIsToday = isToday,
            isPlanning = isPlanning,
            initialMeal = activeMealSlot,
            energyPlan = energyPlan,
            energyBalance = energyBalance,
            dayTotals = dayTotals,
            isSoundMuted = isSoundMuted,
            onDismiss = { configuringFood = null },
            onLog = { meal, grams ->
                val entry = food.toFoodLogEntry(meal = meal, grams = grams)
                onFoodLogged(entry, food)
                configuringFood = null
            },
        )
    }
}

@Composable
private fun EquationStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = color,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MacroDotValue(label: String, value: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Surface(shape = CircleShape, color = color, modifier = Modifier.size(5.dp)) {}
        Text(
            text = "$label: $value",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FoodDatabaseCard(
    food: CuratedFoodItem,
    isFavorite: Boolean,
    activeMealSlot: MealSlot,
    onOpen: () -> Unit,
    onQuickLog: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpen),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FoodThumbnail(food = food, modifier = Modifier.size(52.dp))

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = food.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val brand = food.brand?.takeIf { it.isNotBlank() && it != "Whole Food / Fresh" }
                    val servingDesc = food.commonServing?.name ?: "100g"
                    Text(
                        text = buildString {
                            if (brand != null) append("$brand • ")
                            append(food.category ?: "Food")
                            append(" • $servingDesc")
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(6.dp),
                        ) {
                            Text(
                                text = "${food.calories.roundToInt()} kcal",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            )
                        }

                        food.nutriscore?.let { FoodNutriScoreBadge(it) }
                        food.nova?.let { FoodNovaBadge(it) }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    // 1-Tap Quick-Log Button
                    Surface(
                        onClick = onQuickLog,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(34.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Quick log to ${activeMealSlot.label}",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    // Favorite Button
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = if (isFavorite) "Remove favorite" else "Add favorite",
                            tint = if (isFavorite) AroTheme.macroColors.carbs else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            // Phase 5: Stacked Macro Micro-Bar
            val totalMacros = (food.protein + food.carbs + food.fat).coerceAtLeast(0.1)
            val pRatio = (food.protein / totalMacros).toFloat()
            val cRatio = (food.carbs / totalMacros).toFloat()
            val fRatio = (food.fat / totalMacros).toFloat()

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                ) {
                    if (pRatio > 0.01f) {
                        Box(
                            modifier = Modifier
                                .weight(pRatio)
                                .fillMaxHeight()
                                .background(AroTheme.macroColors.protein),
                        )
                    }
                    if (cRatio > 0.01f) {
                        Box(
                            modifier = Modifier
                                .weight(cRatio)
                                .fillMaxHeight()
                                .background(AroTheme.macroColors.carbs),
                        )
                    }
                    if (fRatio > 0.01f) {
                        Box(
                            modifier = Modifier
                                .weight(fRatio)
                                .fillMaxHeight()
                                .background(AroTheme.macroColors.fat),
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MacroDotValue("P", "${food.protein.roundToInt()}g", AroTheme.macroColors.protein)
                        MacroDotValue("C", "${food.carbs.roundToInt()}g", AroTheme.macroColors.carbs)
                        MacroDotValue("F", "${food.fat.roundToInt()}g", AroTheme.macroColors.fat)
                    }

                    Text(
                        text = "per 100g",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

@Composable
private fun FoodThumbnail(food: CuratedFoodItem, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Restaurant,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(26.dp),
        )
        if (!food.image.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(food.image)
                    .crossfade(true)
                    .build(),
                contentDescription = food.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp)),
            )
        }
    }
}

@Composable
private fun MacroLabel(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium, fontSize = 10.sp),
        color = color,
    )
}

@Composable
private fun FoodNutriScoreBadge(grade: String) {
    val color = when (grade.lowercase()) {
        "a" -> Color(0xFF038141)
        "b" -> Color(0xFF85BB2F)
        "c" -> Color(0xFFFECB02)
        "d" -> Color(0xFFEE8100)
        "e" -> Color(0xFFE63E11)
        else -> Color.Gray
    }
    Surface(color = color, shape = RoundedCornerShape(4.dp)) {
        Text(
            text = "SCORE ${grade.uppercase()}",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun FoodNovaBadge(group: Int) {
    val color = when (group) {
        1 -> Color(0xFF038141)
        2 -> Color(0xFF85BB2F)
        3 -> Color(0xFFEE8100)
        4 -> Color(0xFFE63E11)
        else -> Color.Gray
    }
    Surface(color = color, shape = RoundedCornerShape(4.dp)) {
        Text(
            text = "NOVA $group",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun EmptyFoodState(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 36.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Restaurant,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.size(44.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(onClick = onAction, shape = RoundedCornerShape(10.dp)) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun MacroTargetProgressRow(
    label: String,
    eatenG: Double,
    targetG: Double,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val progress = if (targetG > 0) (eatenG / targetG).toFloat().coerceIn(0f, 1f) else 0f
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${eatenG.roundToInt()}/${targetG.roundToInt()}g",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.2f),
        )
    }
}

@Composable
private fun MealSlotCard(
    slot: MealSlot,
    emoji: String,
    isSelected: Boolean,
    cals: Int,
    count: Int,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
        },
        border = BorderStroke(
            1.5.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(emoji, fontSize = 13.sp)
                    Text(
                        text = slot.label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 12.sp,
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (isSelected) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(6.dp),
                    ) {}
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = "$cals kcal",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (count == 1) "1 item" else "$count items",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StepperButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = modifier.height(44.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun PortionPickerDialog(
    food: CuratedFoodItem,
    targetDateLabel: String,
    targetDateIsToday: Boolean,
    isPlanning: Boolean = false,
    initialMeal: MealSlot = MealSlot.BREAKFAST,
    energyPlan: EnergyPlan? = null,
    energyBalance: EnergyBalance? = null,
    dayTotals: NutrientTotals? = null,
    isSoundMuted: Boolean = false,
    onDismiss: () -> Unit,
    onLog: (MealSlot, Double) -> Unit,
) {
    var meal by remember { mutableStateOf(initialMeal) }
    val baseServingGrams = remember(food) { food.commonServing?.grams ?: 100.0 }
    var gramsText by remember {
        mutableStateOf(baseServingGrams.roundToInt().toString())
    }

    val grams = gramsText.toDoubleOrNull()?.coerceAtLeast(1.0) ?: baseServingGrams
    val preview = food.toFoodLogEntry(meal = meal, grams = grams)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        AroDialogSurface(floatingCard = true) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Header Row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FoodThumbnail(food = food, modifier = Modifier.size(54.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = food.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${food.brand ?: "Whole Food"} • ${food.calories.roundToInt()} kcal / 100g",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                AroLoggingDateLabel(
                    formattedDate = targetDateLabel,
                    isToday = targetDateIsToday,
                    labelPrefix = if (isPlanning) "Planning for" else "Logging for",
                )

                // Meal Slot Selector Row
                Text(
                    text = "TARGET MEAL",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(MealSlot.ORDERED, key = { it.name }) { slot ->
                        FilterChip(
                            selected = meal == slot,
                            onClick = {
                                meal = slot
                                BodySoundHaptics.playClickSound(isSoundMuted)
                            },
                            label = { Text(slot.label, style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(8.dp),
                        )
                    }
                }

                // Serving Multipliers Preset Row
                Text(
                    text = "SERVING PRESETS",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    food.commonServing?.let { serving ->
                        item(key = "common_serving") {
                            val isSelected = kotlin.math.abs(grams - serving.grams) < 0.5
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    gramsText = serving.grams.roundToInt().toString()
                                    BodySoundHaptics.playClickSound(isSoundMuted)
                                },
                                label = {
                                    Text(
                                        "${serving.name} (${serving.grams.roundToInt()}g)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                            )
                        }
                    }

                    item(key = "100g_preset") {
                        val isSelected = kotlin.math.abs(grams - 100.0) < 0.5
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                gramsText = "100"
                                BodySoundHaptics.playClickSound(isSoundMuted)
                            },
                            label = { Text("100g", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)) },
                            shape = RoundedCornerShape(8.dp),
                        )
                    }

                    listOf(0.5, 1.0, 1.5, 2.0).forEach { mult ->
                        val targetGrams = (baseServingGrams * mult).roundToInt()
                        item(key = "mult_$mult") {
                            val isSelected = kotlin.math.abs(grams - targetGrams) < 0.5
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    gramsText = targetGrams.toString()
                                    BodySoundHaptics.playClickSound(isSoundMuted)
                                },
                                label = {
                                    Text(
                                        "${mult}x (${targetGrams}g)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                            )
                        }
                    }
                }

                // Tactile Portion Stepper & Input Row
                Text(
                    text = "CUSTOM PORTION",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    StepperButton(
                        label = "-25",
                        onClick = {
                            val next = (grams - 25.0).coerceAtLeast(5.0).roundToInt()
                            gramsText = next.toString()
                            BodySoundHaptics.playClickSound(isSoundMuted)
                        },
                        modifier = Modifier.weight(1f),
                    )
                    StepperButton(
                        label = "-10",
                        onClick = {
                            val next = (grams - 10.0).coerceAtLeast(5.0).roundToInt()
                            gramsText = next.toString()
                            BodySoundHaptics.playClickSound(isSoundMuted)
                        },
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = gramsText,
                        onValueChange = { input ->
                            if (input.length <= 5 && input.all { it.isDigit() }) gramsText = input
                        },
                        suffix = { Text("g", style = MaterialTheme.typography.labelSmall) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(2f),
                        shape = RoundedCornerShape(12.dp),
                    )
                    StepperButton(
                        label = "+10",
                        onClick = {
                            val next = (grams + 10.0).coerceAtMost(5000.0).roundToInt()
                            gramsText = next.toString()
                            BodySoundHaptics.playClickSound(isSoundMuted)
                        },
                        modifier = Modifier.weight(1f),
                    )
                    StepperButton(
                        label = "+25",
                        onClick = {
                            val next = (grams + 25.0).coerceAtMost(5000.0).roundToInt()
                            gramsText = next.toString()
                            BodySoundHaptics.playClickSound(isSoundMuted)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }

                // Portion Nutrition Card
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "NUTRITION FOR ${grams.roundToInt()}g",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                        ) {
                            PortionStat("Calories", "${preview.calories.roundToInt()}", AroTheme.macroColors.calories)
                            PortionStat("Protein", "${preview.proteinG.roundToInt()}g", AroTheme.macroColors.protein)
                            PortionStat("Carbs", "${preview.nutrients.carbsG.roundToInt()}g", AroTheme.macroColors.carbs)
                            PortionStat("Fat", "${preview.nutrients.fatG.roundToInt()}g", AroTheme.macroColors.fat)
                        }
                    }
                }

                // Live Daily Budget Impact Meter
                val targetCal = energyPlan?.targetCalories ?: (energyBalance?.targetCalories ?: 2000)
                val eatenCal = dayTotals?.calories?.roundToInt() ?: (energyBalance?.caloriesEaten ?: 0)
                val portionCal = preview.calories.roundToInt()
                val newTotalCal = eatenCal + portionCal
                val remainingBefore = (targetCal - eatenCal).coerceAtLeast(0)
                val remainingAfter = (targetCal - newTotalCal).coerceAtLeast(0)
                val isOverBudget = newTotalCal > targetCal && targetCal > 0

                val targetProt = energyPlan?.macros?.proteinG ?: 150
                val eatenProt = dayTotals?.proteinG?.roundToInt() ?: 0
                val portionProt = preview.proteinG.roundToInt()
                val newTotalProt = eatenProt + portionProt

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "DAILY BUDGET IMPACT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                ),
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = if (isOverBudget) {
                                    "+${newTotalCal - targetCal} kcal over budget"
                                } else {
                                    "$remainingAfter kcal remaining"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                ),
                                color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        // Shift Progress Bar (Base consumed vs portion addition)
                        val totalDisplay = maxOf(targetCal, newTotalCal).toFloat()
                        val eatenRatio = if (totalDisplay > 0) (eatenCal.toFloat() / totalDisplay).coerceIn(0f, 1f) else 0f
                        val portionRatio = if (totalDisplay > 0) (portionCal.toFloat() / totalDisplay).coerceIn(0f, 1f - eatenRatio) else 0f
                        val remainderRatio = (1f - eatenRatio - portionRatio).coerceAtLeast(0f)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        ) {
                            if (eatenRatio > 0.005f) {
                                Box(
                                    modifier = Modifier
                                        .weight(eatenRatio)
                                        .fillMaxHeight()
                                        .background(AroTheme.macroColors.calories.copy(alpha = 0.45f)),
                                )
                            }
                            if (portionRatio > 0.005f) {
                                Box(
                                    modifier = Modifier
                                        .weight(portionRatio)
                                        .fillMaxHeight()
                                        .background(if (isOverBudget) MaterialTheme.colorScheme.error else AroTheme.macroColors.calories),
                                )
                            }
                            if (remainderRatio > 0.005f) {
                                Box(
                                    modifier = Modifier
                                        .weight(remainderRatio)
                                        .fillMaxHeight()
                                        .background(Color.Transparent),
                                )
                            }
                        }

                        // Macro impact comparison
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Calories: $eatenCal -> $newTotalCal / $targetCal kcal",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "Protein: $eatenProt -> ${newTotalProt}g / ${targetProt}g",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                                color = AroTheme.macroColors.protein,
                            )
                        }
                    }
                }

                // Log Button
                Button(
                    onClick = {
                        BodySoundHaptics.playClickSound(isSoundMuted)
                        onLog(meal, grams)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = if (isPlanning) {
                            "Plan ${grams.roundToInt()}g to ${meal.label}"
                        } else {
                            "Log ${grams.roundToInt()}g to ${meal.label}"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }
        }
    }
}

@Composable
private fun PortionStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = color,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
