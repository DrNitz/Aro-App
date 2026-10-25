package com.drnitz.aro.ui.food

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.drnitz.aro.domain.model.CuratedFoodItem
import com.drnitz.aro.domain.model.FoodLogEntry
import com.drnitz.aro.domain.model.MealSlot
import com.drnitz.aro.theme.AroTheme
import com.drnitz.aro.ui.components.AroDialogSurface
import com.drnitz.aro.ui.components.AroLoggingDateChip
import com.drnitz.aro.ui.components.AroLoggingDateLabel
import com.drnitz.aro.ui.components.BarcodeScannerView
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
) {
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

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "food_header_controls") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AroLoggingDateChip(
                    selectedDate = selectedDate,
                    formattedDate = formattedDate,
                    isToday = isToday,
                    onSelectDate = onSelectDate,
                    onToday = onToday,
                    labelPrefix = if (isPlanning) "Planning for" else "Logging for",
                )

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = {
                        Text(
                            text = if (source == FoodSource.ONLINE) "Search Open Food Facts..." else "Search 40+ curated foods...",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                                }
                            }
                            IconButton(onClick = { showScanner = true }, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Scan barcode",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = source == FoodSource.OFFLINE,
                        onClick = { source = FoodSource.OFFLINE },
                        label = { Text("Offline Whole Foods", style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(8.dp),
                    )
                    FilterChip(
                        selected = source == FoodSource.ONLINE,
                        onClick = { source = FoodSource.ONLINE },
                        label = { Text("Open Food Facts", style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(8.dp),
                    )
                }

                if (source == FoodSource.OFFLINE) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(FoodFilter.entries.toList(), key = { it.name }) { option ->
                            FilterChip(
                                selected = filter == option,
                                onClick = { filter = option },
                                leadingIcon = if (option == FoodFilter.FAVORITES) {
                                    {
                                        Icon(
                                            imageVector = if (filter == option) Icons.Default.Star else Icons.Default.StarBorder,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp),
                                        )
                                    }
                                } else {
                                    null
                                },
                                label = { Text(option.label, style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(8.dp),
                            )
                        }
                    }

                    if (filter == FoodFilter.ALL || filter == FoodFilter.CURATED) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(categories, key = { it }) { category ->
                                FilterChip(
                                    selected = selectedCategory == category,
                                    onClick = { selectedCategory = category },
                                    label = { Text(category, style = MaterialTheme.typography.labelSmall) },
                                    shape = RoundedCornerShape(8.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        when {
            source == FoodSource.ONLINE && isOnlineLoading -> {
                item(key = "food_online_loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Searching Open Food Facts...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            source == FoodSource.OFFLINE && isOfflineLoading && displayResults.isEmpty() -> {
                item(key = "food_offline_loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            (source == FoodSource.ONLINE && query.trim().length < 2) ||
                (source == FoodSource.OFFLINE &&
                    filter == FoodFilter.FAVORITES &&
                    favoriteFoods.isEmpty()) ||
                (source == FoodSource.OFFLINE &&
                    filter == FoodFilter.RECENT &&
                    recentFoods.isEmpty()) -> {
                item(key = "food_empty_prompt") {
                    EmptyFoodState(
                        title = when {
                            filter == FoodFilter.FAVORITES -> "No favorites yet"
                            filter == FoodFilter.RECENT -> "Nothing logged recently"
                            else -> "Search the Open Food Facts catalogue"
                        },
                        subtitle = when {
                            filter == FoodFilter.FAVORITES -> "Tap the star on any food to keep it one tap away."
                            filter == FoodFilter.RECENT -> "Foods you log will appear here automatically."
                            else -> "Type at least two characters, or scan a barcode."
                        },
                    )
                }
            }

            searchError != null && displayResults.isEmpty() -> {
                item(key = "food_empty_error") {
                    EmptyFoodState(
                        title = "No results",
                        subtitle = searchError!!,
                        actionLabel = if (source == FoodSource.ONLINE) null else "Search Open Food Facts",
                        onAction = { source = FoodSource.ONLINE },
                    )
                }
            }

            displayResults.isEmpty() -> {
                item(key = "food_empty_not_found") {
                    EmptyFoodState(
                        title = if (query.isBlank()) "No foods available" else "No foods match \"$query\"",
                        subtitle = if (source == FoodSource.OFFLINE) {
                            "The curated library is bundled offline. Try Open Food Facts for branded products."
                        } else {
                            "Try a different spelling or scan the package barcode."
                        },
                        actionLabel = if (source == FoodSource.OFFLINE) "Search Open Food Facts" else null,
                        onAction = { source = FoodSource.ONLINE },
                    )
                }
            }

            else -> {
                searchError?.let { error ->
                    item(key = "food_search_error_banner") {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = error,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                    }
                }

                item(key = "food_count_label") {
                    Text(
                        text = "${displayResults.size} ${if (displayResults.size == 1) "food" else "foods"}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }

                items(displayResults, key = { it.id }) { food ->
                    FoodDatabaseCard(
                        food = food,
                        isFavorite = food.id in favoriteIds,
                        onOpen = { configuringFood = food },
                        onToggleFavorite = { onToggleFavorite(food) },
                    )
                }

                item(key = "food_bottom_spacing") {
                    Spacer(modifier = Modifier.height(10.dp))
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
private fun FoodDatabaseCard(
    food: CuratedFoodItem,
    isFavorite: Boolean,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpen),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FoodThumbnail(food = food, modifier = Modifier.size(64.dp))

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = food.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val brand = food.brand?.takeIf { it.isNotBlank() && it != "Whole Food / Fresh" }
                Text(
                    text = buildString {
                        if (brand != null) append(brand)
                        if (brand != null && food.category != null) append(" • ")
                        append(food.category ?: "Food")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(5.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MacroLabel("${food.calories.roundToInt()} kcal", AroTheme.macroColors.calories)
                    MacroLabel("P ${food.protein.roundToInt()}g", AroTheme.macroColors.protein)
                    MacroLabel("C ${food.carbs.roundToInt()}g", AroTheme.macroColors.carbs)
                    MacroLabel("F ${food.fat.roundToInt()}g", AroTheme.macroColors.fat)
                }

                if (food.nutriscore != null || food.nova != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        food.nutriscore?.let { FoodNutriScoreBadge(it) }
                        food.nova?.let { FoodNovaBadge(it) }
                    }
                }
            }

            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (isFavorite) "Remove favorite" else "Add favorite",
                    tint = if (isFavorite) AroTheme.macroColors.carbs else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
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
private fun PortionPickerDialog(
    food: CuratedFoodItem,
    targetDateLabel: String,
    targetDateIsToday: Boolean,
    isPlanning: Boolean = false,
    onDismiss: () -> Unit,
    onLog: (MealSlot, Double) -> Unit,
) {
    val suggestedMeal = remember {
        when (LocalTime.now().hour) {
            in 5..10 -> MealSlot.BREAKFAST
            in 11..15 -> MealSlot.LUNCH
            in 16..21 -> MealSlot.DINNER
            else -> MealSlot.SNACKS
        }
    }
    var meal by remember { mutableStateOf(suggestedMeal) }
    var gramsText by remember {
        mutableStateOf((food.commonServing?.grams ?: 100.0).roundToInt().toString())
    }

    val grams = gramsText.toDoubleOrNull()?.coerceAtLeast(1.0) ?: 100.0
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FoodThumbnail(food = food, modifier = Modifier.size(58.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = food.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "${food.brand ?: "Whole Food"} • ${food.calories.roundToInt()} kcal / 100g",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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

                Text(
                    text = "MEAL",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(MealSlot.ORDERED, key = { it.name }) { slot ->
                        FilterChip(
                            selected = meal == slot,
                            onClick = { meal = slot },
                            label = { Text(slot.label, style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(8.dp),
                        )
                    }
                }

                Text(
                    text = "PORTION",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    food.commonServing?.let { serving ->
                        OutlinedButton(
                            onClick = { gramsText = serving.grams.roundToInt().toString() },
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = "${serving.name} (${serving.grams.roundToInt()}g)",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                            )
                        }
                    }
                    OutlinedButton(onClick = { gramsText = "100" }, shape = RoundedCornerShape(8.dp)) {
                        Text("100g", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(onClick = { gramsText = "200" }, shape = RoundedCornerShape(8.dp)) {
                        Text("200g", style = MaterialTheme.typography.labelSmall)
                    }
                }

                OutlinedTextField(
                    value = gramsText,
                    onValueChange = { input ->
                        if (input.length <= 5 && input.all { it.isDigit() }) gramsText = input
                    },
                    label = { Text("Grams") },
                    suffix = { Text("g") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )

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

                Button(
                    onClick = { onLog(meal, grams) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = if (isPlanning) {
                            "Plan to ${meal.label}"
                        } else {
                            "Log to ${meal.label}"
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
