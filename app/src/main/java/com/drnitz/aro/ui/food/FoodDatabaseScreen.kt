package com.drnitz.aro.ui.food

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import com.drnitz.aro.domain.model.MicronutrientCatalog
import com.drnitz.aro.domain.model.MicronutrientGroup
import com.drnitz.aro.domain.model.NutrientAmount
import com.drnitz.aro.domain.model.NutrientKey
import com.drnitz.aro.domain.model.NutrientTotals
import com.drnitz.aro.theme.AroTheme
import com.drnitz.aro.ui.bodymap.BodySoundHaptics
import com.drnitz.aro.ui.components.AroDialogSurface
import com.drnitz.aro.ui.components.AroLoggingDateChip
import com.drnitz.aro.ui.components.AroLoggingDateLabel
import com.drnitz.aro.ui.components.BarcodeScannerView
import com.drnitz.aro.ui.components.ConcentricMacroRings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class FoodSource { OFFLINE, ONLINE }

private enum class FoodFilter(val label: String) {
    ALL("All"),
    FAVORITES("Favorites"),
    RECENT("Recent"),
}

private val MealSlot.shortLabel: String
    get() = when (this) {
        MealSlot.BREAKFAST -> "Breakfast"
        MealSlot.LUNCH -> "Lunch"
        MealSlot.DINNER -> "Dinner"
        MealSlot.SNACKS -> "Snacks"
    }

private val MealSlot.emoji: String
    get() = when (this) {
        MealSlot.BREAKFAST -> "🍳"
        MealSlot.LUNCH -> "🥗"
        MealSlot.DINNER -> "🍲"
        MealSlot.SNACKS -> "🍎"
    }

private fun MealSlot.next(): MealSlot = when (this) {
    MealSlot.BREAKFAST -> MealSlot.LUNCH
    MealSlot.LUNCH -> MealSlot.DINNER
    MealSlot.DINNER -> MealSlot.SNACKS
    MealSlot.SNACKS -> MealSlot.BREAKFAST
}

/**
 * Dedicated Food & Nutrition tab.
 *
 * Mirrors the web food database: image cards with Nutri-Score/NOVA badges, instant
 * search, favourite/recents filters and a barcode shortcut. Offline whole foods remain
 * the default source; Open Food Facts is an explicit opt-in so the app never requires a
 * network to log a meal.
 *
 * The card preview and the one-tap log button both use [CuratedFoodItem.defaultServingGrams],
 * so the numbers on screen are exactly the numbers written to the diary. Nutrients the
 * source never reported render as "—" and stay unreported in the domain model rather
 * than silently becoming a known zero.
 */
@OptIn(ExperimentalLayoutApi::class)
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

    // Smooth 3D card rotation between Macro Fuel Gauge (0°) and Meal Distribution (180°).
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
    val effectiveFiberTarget = energyPlan?.macros?.fiberG?.toDouble() ?: 30.0

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
    var searchRetry by remember { mutableStateOf(0) }

    var configuringFood by remember { mutableStateOf<CuratedFoodItem?>(null) }
    var detailNotice by remember { mutableStateOf<String?>(null) }
    var openingFoodId by remember { mutableStateOf<String?>(null) }
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

    LaunchedEffect(query, source, searchRetry) {
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            searchError = e.message ?: "Open Food Facts search failed. Check your connection and try again."
        } finally {
            isOnlineLoading = false
        }
    }

    val displayResults: List<CuratedFoodItem> = when {
        source == FoodSource.ONLINE -> onlineResults
        filter == FoodFilter.FAVORITES -> enrichedFavorites.filter { it.matchesQuery(query) }
        filter == FoodFilter.RECENT -> enrichedRecents.filter { it.matchesQuery(query) }
        else -> (offlineResults + enrichedFavorites + enrichedRecents)
            .filter { it.matchesQuery(query) && it.matchesCategory(selectedCategory) }
            .distinctBy { it.id }
    }

    /** Opens a search hit immediately, then swaps in the full barcode label when available. */
    fun openFoodDetails(food: CuratedFoodItem) {
        val code = food.code
        // Saved OFF favourites/recents keep their barcode, so they rehydrate the same way.
        val needsEnrichment = openFoodFactsClient != null &&
            !code.isNullOrBlank() &&
            (food.ingredientsText == null || food.micronutrients.isEmpty() || food.commonServing == null)

        if (!needsEnrichment) {
            detailNotice = null
            configuringFood = food
            return
        }

        openingFoodId = food.id
        scope.launch {
            try {
                val full = openFoodFactsClient?.getProductByBarcode(code)
                detailNotice = if (full == null) {
                    "Full label unavailable — showing search-result data only."
                } else {
                    null
                }
                configuringFood = full ?: food
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                detailNotice = "Could not load the full label: ${e.message ?: "network error"}. Showing search-result data."
                configuringFood = food
            } finally {
                openingFoodId = null
            }
        }
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
                        try {
                            val offline = foodLibrary.findByBarcode(barcode)
                            if (offline != null) {
                                configuringFood = offline
                                showScanner = false
                                return@launch
                            }
                            val client = openFoodFactsClient
                            if (client == null) {
                                searchError = "Online lookup is unavailable. Search the offline catalog for barcode $barcode."
                                showScanner = false
                                drawerExpanded = true
                                return@launch
                            }
                            val online = client.getProductByBarcode(barcode)
                            if (online != null) {
                                configuringFood = online
                                showScanner = false
                                return@launch
                            }
                            searchError = "No product found for barcode $barcode."
                            showScanner = false
                            drawerExpanded = true
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            searchError = e.message ?: "Barcode lookup failed. Check your connection."
                            showScanner = false
                            drawerExpanded = true
                        }
                    }
                },
                onClose = { showScanner = false },
            )
        }
    }

    val currentMealCals = diaryDay?.totalsFor(activeMealSlot)?.calories?.roundToInt() ?: 0

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header: weighted date chip + compact active-meal pill. No side dock overlaps.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 8.dp),
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
                    modifier = Modifier.weight(1f),
                )

                MealContextPill(
                    meal = activeMealSlot,
                    calories = currentMealCals,
                    onClick = {
                        activeMealSlot = activeMealSlot.next()
                        BodySoundHaptics.playClickSound(isSoundMuted)
                    },
                )
            }

            FoodActionStrip(
                isMealShowing = isMealDistributionShowing,
                isOnline = source == FoodSource.ONLINE,
                isMuted = isSoundMuted,
                onFlip = {
                    isMealDistributionShowing = !isMealDistributionShowing
                    BodySoundHaptics.playClickSound(isSoundMuted)
                },
                onScan = {
                    showScanner = true
                    BodySoundHaptics.playClickSound(isSoundMuted)
                },
                onToggleSource = {
                    source = if (source == FoodSource.OFFLINE) FoodSource.ONLINE else FoodSource.OFFLINE
                    BodySoundHaptics.playClickSound(isSoundMuted)
                },
                onToggleSound = { isSoundMuted = !isSoundMuted },
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            )

            // Centre-stage visualizer with 3D flip.
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(184.dp)
                    .padding(horizontal = 14.dp)
                    .graphicsLayer {
                        rotationY = if (isFrontShowing) rotation else rotation - 180f
                        cameraDistance = 14f * density
                    },
            ) {
                if (isFrontShowing) {
                    // FRONT: Concentric macro rings + macro fuel target bars.
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
                    // BACK: 2×2 meal distribution grid.
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
                                emoji = MealSlot.BREAKFAST.emoji,
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
                                emoji = MealSlot.LUNCH.emoji,
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
                                emoji = MealSlot.DINNER.emoji,
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
                                emoji = MealSlot.SNACKS.emoji,
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

            // Scrollable summary: active meal, nutrient coverage and the energy equation.
            val currentMealEntries = diaryDay?.meals?.get(activeMealSlot).orEmpty()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 80.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
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
                                text = "Nothing logged for ${activeMealSlot.label} yet. Open the catalog below or tap Add Food to search whole foods.",
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

                DailyNutrientCoverageCard(
                    dayTotals = dayTotals,
                    targetCalories = effectiveTargetCalories,
                    fiberTargetG = effectiveFiberTarget,
                    modifier = Modifier.fillMaxWidth(),
                )

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

            }
        }

        // Collapsed bottom peek bar.
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
                                text = if (selectedCategory != "All") {
                                    "$selectedCategory Foods"
                                } else if (source == FoodSource.ONLINE) {
                                    "Open Food Facts"
                                } else {
                                    "Food Catalog"
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "Log to ${activeMealSlot.shortLabel} • ${displayResults.size} items • Tap to browse",
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

        // Expanded bottom food drawer.
        AnimatedVisibility(
            visible = drawerExpanded,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            val drawerHeightFraction = if (drawerFullScreen) 0.92f else 0.64f

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
                                .height(44.dp),
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
                                            text = if (source == FoodSource.ONLINE) {
                                                "Search Open Food Facts..."
                                            } else if (allCuratedFoods.isEmpty()) {
                                                "Search whole foods..."
                                            } else {
                                                "Search ${allCuratedFoods.size} whole foods..."
                                            },
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

                    // Row 1: source + collection filter. FlowRow keeps every chip fully
                    // visible at 360dp instead of clipping the last one in a scroller.
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        FilterChip(
                            selected = source == FoodSource.ONLINE,
                            onClick = {
                                source = if (source == FoodSource.OFFLINE) FoodSource.ONLINE else FoodSource.OFFLINE
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (source == FoodSource.ONLINE) Icons.Default.Language else Icons.Default.Restaurant,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                )
                            },
                            label = {
                                Text(
                                    if (source == FoodSource.ONLINE) "Open Food Facts" else "Whole foods",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                        )

                        if (source == FoodSource.OFFLINE) {
                            FoodFilter.entries.forEach { option ->
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
                                    label = {
                                        Text(
                                            option.label,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        )
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                )
                            }
                        }
                    }

                    // Row 2: categories (whole foods only).
                    if (source == FoodSource.OFFLINE && filter == FoodFilter.ALL) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            items(categories, key = { "cat_$it" }) { category ->
                                FilterChip(
                                    selected = selectedCategory == category,
                                    onClick = { selectedCategory = category },
                                    label = {
                                        Text(
                                            category,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        )
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                )
                            }
                        }
                    } else if (source == FoodSource.ONLINE) {
                        Text(
                            text = "Live Open Food Facts results. Tap a product to load its full label and micronutrients.",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }

                    Spacer(Modifier.height(4.dp))

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
                                            CircularProgressIndicator(
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(32.dp),
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                "Searching Open Food Facts...",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
                            source == FoodSource.ONLINE && query.trim().length < 2 -> {
                                item(key = "online_prompt") {
                                    EmptyFoodState(
                                        title = "Search Open Food Facts",
                                        subtitle = "Type at least two characters or scan a barcode. Online results need an internet connection.",
                                    )
                                }
                            }
                            source == FoodSource.OFFLINE && isOfflineLoading && displayResults.isEmpty() -> {
                                item(key = "offline_loading") {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp),
                                        )
                                    }
                                }
                            }
                            source == FoodSource.OFFLINE && filter == FoodFilter.FAVORITES && enrichedFavorites.isEmpty() -> {
                                item(key = "empty_favorites") {
                                    EmptyFoodState(
                                        title = "No favorites yet",
                                        subtitle = "Tap the star on any food to keep it one tap away.",
                                    )
                                }
                            }
                            source == FoodSource.OFFLINE && filter == FoodFilter.RECENT && enrichedRecents.isEmpty() -> {
                                item(key = "empty_recents") {
                                    EmptyFoodState(
                                        title = "Nothing logged recently",
                                        subtitle = "Foods you log will appear here automatically.",
                                    )
                                }
                            }
                            searchError != null && displayResults.isEmpty() -> {
                                item(key = "empty_error") {
                                    EmptyFoodState(
                                        title = if (source == FoodSource.ONLINE) "Open Food Facts unavailable" else "No results",
                                        subtitle = searchError!!,
                                        actionLabel = if (source == FoodSource.ONLINE) "Try again" else "Search Open Food Facts",
                                        onAction = {
                                            if (source == FoodSource.ONLINE) {
                                                openFoodFactsClient?.clearSearchCache()
                                                searchRetry++
                                            } else {
                                                source = FoodSource.ONLINE
                                            }
                                        },
                                    )
                                }
                            }
                            displayResults.isEmpty() -> {
                                item(key = "empty_not_found") {
                                    EmptyFoodState(
                                        title = if (query.isBlank()) {
                                            if (selectedCategory != "All") "No foods in $selectedCategory" else "No foods available"
                                        } else {
                                            "No foods match \"$query\""
                                        },
                                        subtitle = if (source == FoodSource.OFFLINE) {
                                            "The curated library is bundled offline. Try Open Food Facts for branded items."
                                        } else {
                                            "Try a different spelling or scan the barcode."
                                        },
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
                                        isOpening = openingFoodId == food.id,
                                        onOpen = { openFoodDetails(food) },
                                        onQuickLog = {
                                            val entry = food.toFoodLogEntry(meal = activeMealSlot)
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
            sourceNotice = detailNotice,
            onRetryFullLabel = if (
                detailNotice != null &&
                !food.code.isNullOrBlank() &&
                openFoodFactsClient != null
            ) {
                { openFoodDetails(food) }
            } else {
                null
            },
            onDismiss = {
                configuringFood = null
                detailNotice = null
            },
            onLog = { meal, grams ->
                val entry = food.toFoodLogEntry(meal = meal, grams = grams)
                onFoodLogged(entry, food)
                configuringFood = null
                detailNotice = null
            },
        )
    }
}

@Composable
private fun MealContextPill(
    meal: MealSlot,
    calories: Int,
    onClick: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        shadowElevation = 2.dp,
        modifier = Modifier
            .clickable(onClick = onClick)
            .heightIn(min = 40.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "${meal.emoji} ${meal.shortLabel}",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
            )
            Text(
                text = "$calories kcal",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun FoodActionStrip(
    isMealShowing: Boolean,
    isOnline: Boolean,
    isMuted: Boolean,
    onFlip: () -> Unit,
    onScan: () -> Unit,
    onToggleSource: () -> Unit,
    onToggleSound: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FoodActionButton(
                icon = Icons.Default.Cached,
                label = if (isMealShowing) "Meals" else "Macros",
                contentDescription = "Flip nutrition card between macros and meals",
                tint = AroTheme.macroColors.carbs,
                onClick = onFlip,
                modifier = Modifier.weight(1f),
            )
            ActionDivider()
            FoodActionButton(
                icon = Icons.Default.QrCodeScanner,
                label = "Scan",
                contentDescription = "Scan a barcode",
                tint = MaterialTheme.colorScheme.primary,
                onClick = onScan,
                modifier = Modifier.weight(1f),
            )
            ActionDivider()
            FoodActionButton(
                icon = if (isOnline) Icons.Default.Language else Icons.Default.Restaurant,
                label = if (isOnline) "Online" else "Offline",
                contentDescription = "Toggle between offline whole foods and Open Food Facts",
                tint = if (isOnline) AroTheme.macroColors.fat else MaterialTheme.colorScheme.primary,
                isActive = isOnline,
                onClick = onToggleSource,
                modifier = Modifier.weight(1f),
            )
            ActionDivider()
            FoodActionButton(
                icon = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                label = if (isMuted) "Muted" else "Sound",
                contentDescription = if (isMuted) "Unmute audio" else "Mute audio",
                tint = if (isMuted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onToggleSound,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ActionDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    )
}

@Composable
private fun FoodActionButton(
    icon: ImageVector,
    label: String,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(17.dp),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
            ),
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
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

/** Compact decimal formatter used by every nutrient surface. */
private fun formatValue(value: Double): String {
    if (!value.isFinite()) return "—"
    val rounded = when {
        abs(value) >= 100.0 -> String.format(Locale.US, "%.0f", value)
        abs(value) >= 10.0 -> String.format(Locale.US, "%.1f", value)
        abs(value) >= 1.0 -> String.format(Locale.US, "%.1f", value)
        else -> String.format(Locale.US, "%.2f", value)
    }
    return if (rounded.contains('.')) rounded.trimEnd('0').trimEnd('.') else rounded
}

/** Never renders an unreported nutrient as "0" — it renders an em dash. */
private fun NutrientAmount.display(unit: String): String {
    val reported = value ?: return "—"
    val text = formatValue(reported)
    return if (unit.isBlank()) text else "$text $unit"
}

private fun CuratedFoodItem.per100Text(key: NutrientKey, unit: String): String =
    per100gNutrients[key].display(unit)

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
    isOpening: Boolean,
    onOpen: () -> Unit,
    onQuickLog: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val servingGrams = food.defaultServingGrams
    val servingLabel = food.commonServing?.name ?: "${formatValue(servingGrams)} g"
    val servingFactor = servingGrams / 100.0

    fun servingValue(key: NutrientKey, unit: String): String {
        val amount = food.per100gNutrients[key]
        val reported = amount.value ?: return "—"
        val text = formatValue(reported * servingFactor)
        return if (unit.isBlank()) text else "$text $unit"
    }

    val totalMacros = (food.protein + food.carbs + food.fat).coerceAtLeast(0.1)
    val pRatio = (food.protein / totalMacros).toFloat()
    val cRatio = (food.carbs / totalMacros).toFloat()
    val fRatio = (food.fat / totalMacros).toFloat()
    val brand = food.brand?.takeIf { it.isNotBlank() && it != "Whole Food / Fresh" }

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
                    Text(
                        text = buildString {
                            if (brand != null) append("$brand • ")
                            append(servingLabel)
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(6.dp),
                        ) {
                            Text(
                                text = "${servingValue(NutrientKey.CALORIES, "kcal")}",
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
                    Surface(
                        onClick = onQuickLog,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(34.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isOpening) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(15.dp),
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Quick log one $servingLabel to ${activeMealSlot.label}",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }

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
                        MacroDotValue("P", servingValue(NutrientKey.PROTEIN, "g"), AroTheme.macroColors.protein)
                        MacroDotValue("C", servingValue(NutrientKey.CARBS, "g"), AroTheme.macroColors.carbs)
                        MacroDotValue("F", servingValue(NutrientKey.FAT, "g"), AroTheme.macroColors.fat)
                    }

                    Text(
                        text = "per $servingLabel",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 96.dp),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MacroDotValue("Fiber", servingValue(NutrientKey.FIBER, "g"), Color(0xFF7CB342))
                    MacroDotValue("Sugar", servingValue(NutrientKey.SUGAR, "g"), AroTheme.macroColors.carbs.copy(alpha = 0.75f))
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
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
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
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
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
            drawStopIndicator = {},
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
                        text = slot.shortLabel,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 12.sp,
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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

private data class CoverageTarget(
    val key: NutrientKey,
    val target: Double,
    val unit: String,
    val upperLimit: Boolean,
    val color: Color,
    val reference: String,
)

@Composable
private fun DailyNutrientCoverageCard(
    dayTotals: NutrientTotals?,
    targetCalories: Int,
    fiberTargetG: Double,
    modifier: Modifier = Modifier,
) {
    val satFatLimit = (targetCalories * 0.10 / 9.0).coerceAtLeast(1.0)
    val freeSugarReference = (targetCalories * 0.10 / 4.0).coerceAtLeast(1.0)

    val targets = listOf(
        CoverageTarget(NutrientKey.FIBER, fiberTargetG, "g", false, Color(0xFF7CB342), "goal"),
        CoverageTarget(NutrientKey.SODIUM, 2300.0, "mg", true, Color(0xFFEF6C00), "limit"),
        CoverageTarget(NutrientKey.SUGAR, freeSugarReference, "g", true, AroTheme.macroColors.carbs, "WHO ref."),
        CoverageTarget(NutrientKey.SATURATED_FAT, satFatLimit, "g", true, AroTheme.macroColors.fat, "limit"),
        CoverageTarget(NutrientKey.POTASSIUM, 3500.0, "mg", false, AroTheme.macroColors.protein, "goal"),
        CoverageTarget(NutrientKey.CALCIUM, 1000.0, "mg", false, Color(0xFF26A69A), "goal"),
        CoverageTarget(NutrientKey.MAGNESIUM, 400.0, "mg", false, Color(0xFF7E57C2), "goal"),
    )
    val reportedCount = NutrientKey.entries.count { dayTotals?.get(it)?.isReported == true }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f),
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
        modifier = modifier,
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
                    text = "DAILY NUTRIENT COVERAGE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp),
                ) {
                    Text(
                        text = "$reportedCount/${NutrientKey.entries.size} with data",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    )
                }
            }

            targets.forEach { target ->
                val amount = dayTotals?.get(target.key) ?: NutrientAmount.UNKNOWN
                val overLimit = target.upperLimit && amount.isReported && amount.orZero > target.target
                val progress = if (target.target > 0) {
                    (amount.orZero / target.target).toFloat().coerceIn(0f, 1f)
                } else {
                    0f
                }
                val barColor = if (overLimit) MaterialTheme.colorScheme.error else target.color

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(modifier = Modifier.width(92.dp)) {
                        Text(
                            text = if (target.key == NutrientKey.SUGAR) "Total sugars" else target.key.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${target.reference} ${formatValue(target.target)} ${target.unit}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = barColor,
                        trackColor = barColor.copy(alpha = 0.18f),
                        // The default end-stop dot reads like a filled progress bar at 0.
                        drawStopIndicator = {},
                    )

                    Column(
                        modifier = Modifier.width(64.dp),
                        horizontalAlignment = Alignment.End,
                    ) {
                        Text(
                            text = if (amount.isReported) {
                                "${formatValue(amount.orZero)} ${target.unit}"
                            } else {
                                "—"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = if (amount.isReported) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                        )
                        if (!amount.isReported) {
                            Text(
                                text = "unreported",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                color = MaterialTheme.colorScheme.outline,
                                maxLines = 1,
                            )
                        } else if (amount.isEstimated) {
                            Text(
                                text = "est.",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                color = MaterialTheme.colorScheme.outline,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }

            Text(
                text = "Missing values are shown as unreported, not as zero; \"est.\" marks a derived or partially reported total. " +
                    "Total sugars include intrinsic sugars, so the WHO <10%-of-energy free-sugar reference is context only, not a strict target.",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
            )
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

private data class ServingPreset(val label: String, val grams: Double)

@OptIn(ExperimentalLayoutApi::class)
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
    sourceNotice: String? = null,
    onRetryFullLabel: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onLog: (MealSlot, Double) -> Unit,
) {
    var meal by remember { mutableStateOf(initialMeal) }
    val baseServingGrams = remember(food) { food.defaultServingGrams }
    var gramsText by remember(food) { mutableStateOf(formatValue(baseServingGrams)) }
    var showKnowMore by remember(food) { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val grams = gramsText.toDoubleOrNull()?.coerceAtLeast(1.0) ?: baseServingGrams
    val preview = food.toFoodLogEntry(meal = meal, grams = grams)
    val unreportedKeys = remember(food) { NutrientKey.entries.filter { !food.isReported(it) } }

    val presets = remember(food, baseServingGrams) {
        val raw = mutableListOf<ServingPreset>()
        food.commonServing?.let { serving ->
            val nameHasGrams = Regex("\\d+(?:\\.\\d+)?\\s*g\\b", RegexOption.IGNORE_CASE)
                .containsMatchIn(serving.name)
            val label = if (nameHasGrams) {
                serving.name
            } else {
                "${serving.name} (${formatValue(serving.grams)} g)"
            }
            raw.add(ServingPreset(label, serving.grams))
        }
        raw.add(ServingPreset("100 g", 100.0))
        listOf(0.5, 1.0, 1.5, 2.0).forEach { multiplier ->
            val target = baseServingGrams * multiplier
            raw.add(ServingPreset("${formatValue(multiplier)}× (${formatValue(target)} g)", target))
        }
        raw.distinctBy { formatValue(it.grams) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        AroDialogSurface(floatingCard = true) {
            Column(modifier = Modifier.fillMaxHeight(0.9f)) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FoodThumbnail(food = food, modifier = Modifier.size(54.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = food.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${food.brand ?: "Whole Food"} • ${food.per100Text(NutrientKey.CALORIES, "kcal")} / 100 g",
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

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        food.nutriscore?.let { FoodNutriScoreBadge(it) }
                        food.nova?.let { FoodNovaBadge(it) }
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            shape = RoundedCornerShape(4.dp),
                        ) {
                            Text(
                                text = if (food.dataSource == "Open Food Facts") "Open Food Facts" else "Aro curated",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            )
                        }
                    }

                    sourceNotice?.let { notice ->
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = notice,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f),
                                )
                                if (onRetryFullLabel != null) {
                                    TextButton(
                                        onClick = onRetryFullLabel,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    ) {
                                        Text(
                                            text = "Retry",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                            ),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    AroLoggingDateLabel(
                        formattedDate = targetDateLabel,
                        isToday = targetDateIsToday,
                        labelPrefix = if (isPlanning) "Planning for" else "Logging for",
                    )

                    Text(
                        text = "TARGET MEAL",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        MealSlot.ORDERED.forEach { slot ->
                            val isSelected = meal == slot
                            Surface(
                                onClick = {
                                    meal = slot
                                    BodySoundHaptics.playClickSound(isSoundMuted)
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    },
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = slot.shortLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        ),
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "SERVING PRESETS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        presets.forEach { preset ->
                            val isSelected = abs(grams - preset.grams) < 0.5
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    gramsText = formatValue(preset.grams)
                                    BodySoundHaptics.playClickSound(isSoundMuted)
                                },
                                label = {
                                    Text(
                                        preset.label,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                            )
                        }
                    }

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
                                gramsText = formatValue((grams - 25.0).coerceAtLeast(5.0))
                                BodySoundHaptics.playClickSound(isSoundMuted)
                            },
                            modifier = Modifier.weight(1f),
                        )
                        StepperButton(
                            label = "-10",
                            onClick = {
                                gramsText = formatValue((grams - 10.0).coerceAtLeast(5.0))
                                BodySoundHaptics.playClickSound(isSoundMuted)
                            },
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = gramsText,
                            onValueChange = { input ->
                                val valid = input.length <= 6 &&
                                    input.count { it == '.' } <= 1 &&
                                    input.all { it.isDigit() || it == '.' }
                                if (valid) gramsText = input
                            },
                            suffix = { Text("g", style = MaterialTheme.typography.labelSmall) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            modifier = Modifier.weight(2f),
                            shape = RoundedCornerShape(12.dp),
                        )
                        StepperButton(
                            label = "+10",
                            onClick = {
                                gramsText = formatValue((grams + 10.0).coerceAtMost(5000.0))
                                BodySoundHaptics.playClickSound(isSoundMuted)
                            },
                            modifier = Modifier.weight(1f),
                        )
                        StepperButton(
                            label = "+25",
                            onClick = {
                                gramsText = formatValue((grams + 25.0).coerceAtMost(5000.0))
                                BodySoundHaptics.playClickSound(isSoundMuted)
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "NUTRITION FOR ${formatValue(grams)} g",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                TextButton(
                                    onClick = {
                                        showKnowMore = !showKnowMore
                                        BodySoundHaptics.playClickSound(isSoundMuted)
                                    },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                ) {
                                    Text(
                                        text = if (showKnowMore) "Hide details" else "Know more",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                        ),
                                    )
                                    Icon(
                                        imageVector = if (showKnowMore) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround,
                            ) {
                                preview.nutrients[NutrientKey.CALORIES].let {
                                    PortionStat("Calories", it.display("kcal"), AroTheme.macroColors.calories, it.isEstimated)
                                }
                                preview.nutrients[NutrientKey.PROTEIN].let {
                                    PortionStat("Protein", it.display("g"), AroTheme.macroColors.protein, it.isEstimated)
                                }
                                preview.nutrients[NutrientKey.CARBS].let {
                                    PortionStat("Carbs", it.display("g"), AroTheme.macroColors.carbs, it.isEstimated)
                                }
                                preview.nutrients[NutrientKey.FAT].let {
                                    PortionStat("Fat", it.display("g"), AroTheme.macroColors.fat, it.isEstimated)
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround,
                            ) {
                                preview.nutrients[NutrientKey.FIBER].let {
                                    PortionStat("Fiber", it.display("g"), Color(0xFF7CB342), it.isEstimated)
                                }
                                preview.nutrients[NutrientKey.SUGAR].let {
                                    PortionStat("Sugar", it.display("g"), AroTheme.macroColors.carbs, it.isEstimated)
                                }
                                preview.nutrients[NutrientKey.SODIUM].let {
                                    PortionStat("Sodium", it.display("mg"), Color(0xFFEF6C00), it.isEstimated)
                                }
                                preview.nutrients[NutrientKey.SATURATED_FAT].let {
                                    PortionStat("Sat fat", it.display("g"), AroTheme.macroColors.fat, it.isEstimated)
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = showKnowMore,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        KnowMorePanel(food = food)
                    }

                    if (unreportedKeys.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text(
                                text = "Not reported by this source: ${unreportedKeys.joinToString(", ") { it.label }}. " +
                                    "Aro counts missing values as 0 in daily totals but does not assume the food is free of them. " +
                                    "\"est.\" marks a value derived from another nutrient.",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            )
                        }
                    }

                    val targetCal = energyPlan?.targetCalories ?: (energyBalance?.targetCalories ?: 2000)
                    val eatenCal = dayTotals?.calories?.roundToInt() ?: (energyBalance?.caloriesEaten ?: 0)
                    val portionCal = preview.calories.roundToInt()
                    val newTotalCal = eatenCal + portionCal
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

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Calories: $eatenCal → $newTotalCal / $targetCal kcal",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "Protein: $eatenProt → ${newTotalProt}g / ${targetProt}g",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                                    color = AroTheme.macroColors.protein,
                                )
                            }
                        }
                    }
                }

                // Fixed footer: the Log button never scrolls away.
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                ) {
                    Button(
                        onClick = {
                            BodySoundHaptics.playClickSound(isSoundMuted)
                            onLog(meal, grams)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = if (isPlanning) {
                                "Plan ${formatValue(grams)} g to ${meal.shortLabel}"
                            } else {
                                "Log ${formatValue(grams)} g to ${meal.shortLabel}"
                            },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PortionStat(label: String, value: String, color: Color, isEstimated: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
            color = color,
            maxLines = 1,
        )
        Text(
            text = if (isEstimated) "$label · est." else label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun KnowMorePanel(food: CuratedFoodItem) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.8f),
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            KnowMoreSection(title = "PER 100 g") {
                val energy = food.per100gNutrients[NutrientKey.CALORIES]
                FactRow(
                    label = "Energy",
                    value = if (energy.isReported) {
                        val kcal = energy.value!!
                        "${formatValue(kcal)} kcal (${formatValue(kcal * 4.184)} kJ)" +
                            if (energy.isEstimated) " · estimated" else ""
                    } else {
                        "Not reported"
                    },
                )
                FactRow("Protein", food.per100Fact(NutrientKey.PROTEIN, "g"))
                FactRow("Carbohydrates", food.per100Fact(NutrientKey.CARBS, "g"))
                FactRow("Fat", food.per100Fact(NutrientKey.FAT, "g"))
                FactRow("Saturated fat", food.per100Fact(NutrientKey.SATURATED_FAT, "g"))
                FactRow("Fiber", food.per100Fact(NutrientKey.FIBER, "g"))
                FactRow("Sugars", food.per100Fact(NutrientKey.SUGAR, "g"))
                FactRow("Sodium", food.per100Fact(NutrientKey.SODIUM, "mg"))
                FactRow("Potassium", food.per100Fact(NutrientKey.POTASSIUM, "mg"))
                FactRow("Calcium", food.per100Fact(NutrientKey.CALCIUM, "mg"))
                FactRow("Magnesium", food.per100Fact(NutrientKey.MAGNESIUM, "mg"))
            }

            val micros = MicronutrientCatalog.ALL.filter { food.micronutrients.containsKey(it.key) }
            if (micros.isNotEmpty()) {
                MicronutrientGroup.entries.forEach { group ->
                    val groupMicros = micros.filter { it.group == group }
                    if (groupMicros.isNotEmpty()) {
                        KnowMoreSection(title = group.label.uppercase()) {
                            groupMicros.forEach { definition ->
                                val value = food.micronutrients[definition.key]
                                if (value != null) {
                                    FactRow(definition.label, "${formatValue(value)} ${definition.unit}")
                                }
                            }
                        }
                    }
                }

                val missingMicroLabels = MicronutrientCatalog.ALL
                    .filter { it.key in food.unreportedMicronutrients }
                    .map { it.label }
                if (missingMicroLabels.isNotEmpty()) {
                    Text(
                        text = "Not reported by this source: ${missingMicroLabels.joinToString(", ")}. " +
                            "They stay absent from the label rather than being assumed zero.",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Text(
                    text = "No extra label micronutrients (iron, vitamins, etc.) were reported for this food.",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            food.ingredientsText?.takeIf { it.isNotBlank() }?.let { ingredients ->
                KnowMoreSection(title = "INGREDIENTS") {
                    Text(
                        text = ingredients,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            if (food.allergens.isNotEmpty() || food.traces.isNotEmpty()) {
                KnowMoreSection(title = "ALLERGENS & TRACES") {
                    if (food.allergens.isNotEmpty()) {
                        FactRow("Allergens", food.allergens.joinToString(", "))
                    }
                    if (food.traces.isNotEmpty()) {
                        FactRow("May contain traces", food.traces.joinToString(", "))
                    }
                }
            }

            if (food.additives.isNotEmpty()) {
                KnowMoreSection(title = "ADDITIVES") {
                    FactRow("Additives", food.additives.joinToString(", "))
                }
            }

            if (food.labels.isNotEmpty() || food.countries.isNotEmpty() || food.packaging.isNotEmpty()) {
                KnowMoreSection(title = "LABELS & PACKAGING") {
                    if (food.labels.isNotEmpty()) FactRow("Labels", food.labels.joinToString(", "))
                    if (food.countries.isNotEmpty()) FactRow("Countries sold", food.countries.joinToString(", "))
                    if (food.packaging.isNotEmpty()) FactRow("Packaging", food.packaging.joinToString(", "))
                }
            }

            KnowMoreSection(title = "DATA & SOURCE") {
                FactRow("Source", food.dataSource)
                food.category?.takeIf { it.isNotBlank() }?.let { FactRow("Category", it) }
                // Only an EAN/UPC-style code is a barcode; curated foods use readable slug IDs.
                food.code
                    ?.takeIf { code -> code.length in 8..14 && code.all(Char::isDigit) }
                    ?.let { FactRow("Barcode", it) }
                food.quantity?.let { FactRow("Pack size", it) }
                food.servingSizeText?.let { FactRow("Serving text", it) }
                food.nutriscore?.let { FactRow("Nutri-Score", it.uppercase()) }
                food.nova?.let { FactRow("NOVA group", "$it") }
                food.ecoscore?.let { FactRow("Eco-Score", it.uppercase()) }
                food.completeness?.let { FactRow("Data completeness", "${(it * 100).roundToInt()}%") }
                food.lastModified?.let { FactRow("Last updated", formatEpochSeconds(it)) }
                FactRow(
                    label = "Nutrients not reported",
                    value = if (food.unreportedNutrients.isEmpty()) {
                        "None"
                    } else {
                        NutrientKey.entries
                            .filter { it.name in food.unreportedNutrients }
                            .joinToString(", ") { it.label }
                            .ifBlank { "None" }
                    },
                )
            }

            val delta = food.energyLabelDeltaPercent
            if (delta != null) {
                KnowMoreSection(title = "SCIENTIFIC CROSS-CHECK") {
                    FactRow("Atwater estimate", "${formatValue(food.atwaterCalories)} kcal (4/4/9 per g)")
                    FactRow(
                        label = "Label vs macros",
                        value = "${if (delta >= 0) "+" else ""}${String.format(Locale.US, "%.1f", delta)}%",
                        isWarning = abs(delta) > 20.0,
                    )
                    if (abs(delta) > 20.0) {
                        Text(
                            text = "A gap over 20% usually means the label rounds energy, includes fiber/polyols, or has incomplete macro data. The calorie figure shown is the published label value.",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            if (food.dataSource == "Open Food Facts") {
                Text(
                    text = "Open Food Facts data © Open Food Facts contributors, available under the Open Database License (ODbL). Values are crowd-sourced; check the pack for medical decisions.",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
private fun KnowMoreSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                letterSpacing = 0.6.sp,
            ),
            color = MaterialTheme.colorScheme.primary,
        )
        content()
    }
}

@Composable
private fun FactRow(label: String, value: String, isWarning: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
            color = when {
                isWarning -> MaterialTheme.colorScheme.error
                value == "Not reported" -> MaterialTheme.colorScheme.outline
                else -> MaterialTheme.colorScheme.onSurface
            },
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(max = 190.dp),
        )
    }
}

private fun CuratedFoodItem.per100Fact(key: NutrientKey, unit: String): String {
    val amount = per100gNutrients[key]
    val reported = amount.value ?: return "Not reported"
    val text = formatValue(reported)
    val suffix = when {
        unit.isBlank() -> ""
        else -> " $unit"
    }
    return text + suffix + if (amount.isEstimated) " · estimated" else ""
}

private fun formatEpochSeconds(seconds: Long): String = runCatching {
    Instant.ofEpochSecond(seconds).atZone(ZoneId.systemDefault()).toLocalDate().toString()
}.getOrDefault("Unknown")
