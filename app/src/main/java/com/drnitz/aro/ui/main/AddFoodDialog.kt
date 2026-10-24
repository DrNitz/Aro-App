package com.drnitz.aro.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.drnitz.aro.data.library.OfflineFoodLibrary
import com.drnitz.aro.data.network.OpenFoodFactsClient
import com.drnitz.aro.domain.model.CuratedFoodItem
import com.drnitz.aro.domain.model.FoodLogEntry
import com.drnitz.aro.domain.model.MealSlot
import com.drnitz.aro.ui.components.AroDialogSurface
import com.drnitz.aro.ui.components.AroDialogWideMinScreenWidthDp
import com.drnitz.aro.ui.components.AroLoggingDateLabel
import com.drnitz.aro.ui.components.BarcodeScannerView
import com.drnitz.aro.theme.AroTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun MacroMiniStat(
    label: String,
    value: String,
    color: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = color,
        )
    }
}

@Composable
internal fun NutriScoreBadge(grade: String) {
    val (bg, label) = when (grade.lowercase()) {
        "a" -> Color(0xFF038141) to "NUTRI-SCORE A"
        "b" -> Color(0xFF85BB2F) to "NUTRI-SCORE B"
        "c" -> Color(0xFFFECB02) to "NUTRI-SCORE C"
        "d" -> Color(0xFFEE8100) to "NUTRI-SCORE D"
        "e" -> Color(0xFFE63E11) to "NUTRI-SCORE E"
        else -> Color.Gray to "NUTRI-SCORE ${grade.uppercase()}"
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
internal fun NovaBadge(group: Int) {
    val bg = when (group) {
        1 -> Color(0xFF038141)
        2 -> Color(0xFF85BB2F)
        3 -> Color(0xFFEE8100)
        4 -> Color(0xFFE63E11)
        else -> Color.Gray
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            text = "NOVA $group",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
internal fun FoodItemCard(
    item: CuratedFoodItem,
    slot: MealSlot,
    onSelect: () -> Unit,
    onQuickAdd: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onSelect),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val subtitle = item.brand?.takeIf { it != "Whole Food / Fresh" }?.let { "$it • " } ?: ""
                val servingSubtitle = item.commonServing?.let {
                    "${it.name} (${it.grams.roundToInt()}g)"
                } ?: "100g serving"
                Text(
                    text = "$subtitle$servingSubtitle • P: ${item.protein.roundToInt()}g  C: ${item.carbs.roundToInt()}g  F: ${item.fat.roundToInt()}g",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (item.nutriscore != null || item.nova != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item.nutriscore?.let { NutriScoreBadge(it) }
                        item.nova?.let { NovaBadge(it) }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${item.calories.roundToInt()} kcal",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = AroTheme.macroColors.calories,
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = onQuickAdd,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Quick add ${item.name}",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun EditPortionDialog(
    entry: FoodLogEntry,
    onDismiss: () -> Unit,
    onConfirm: (FoodLogEntry) -> Unit,
) {
    var gramsText by remember { mutableStateOf(entry.grams.roundToInt().toString()) }
    val currentGrams = gramsText.toDoubleOrNull()?.coerceAtLeast(1.0) ?: entry.grams
    val updatedEntry = entry.withGrams(currentGrams)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Adjust Portion",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                OutlinedTextField(
                    value = gramsText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() } && input.length <= 5) {
                            gramsText = input
                        }
                    },
                    label = { Text("Portion (grams)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(-50, -10, 10, 50).forEach { delta ->
                        OutlinedButton(
                            onClick = {
                                val next = (currentGrams + delta).coerceAtLeast(1.0)
                                gramsText = next.roundToInt().toString()
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp),
                        ) {
                            Text(
                                text = if (delta > 0) "+$delta" else "$delta",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                    ) {
                        MacroMiniStat(
                            label = "Calories",
                            value = "${updatedEntry.calories.roundToInt()} kcal",
                            color = AroTheme.macroColors.calories,
                        )
                        MacroMiniStat(
                            label = "Protein",
                            value = "${updatedEntry.proteinG.roundToInt()}g",
                            color = AroTheme.macroColors.protein,
                        )
                        MacroMiniStat(
                            label = "Carbs",
                            value = "${updatedEntry.nutrients.carbsG.roundToInt()}g",
                            color = AroTheme.macroColors.carbs,
                        )
                        MacroMiniStat(
                            label = "Fat",
                            value = "${updatedEntry.nutrients.fatG.roundToInt()}g",
                            color = AroTheme.macroColors.fat,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(updatedEntry) },
                shape = RoundedCornerShape(10.dp),
            ) {
                Text("Save Portion")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
            ) {
                Text("Cancel")
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddFoodDialog(
    slot: MealSlot,
    foodLibrary: OfflineFoodLibrary,
    openFoodFactsClient: OpenFoodFactsClient? = null,
    targetDateLabel: String = "",
    targetDateIsToday: Boolean = true,
    planningMode: Boolean = false,
    onRecordUsage: (CuratedFoodItem, Double) -> Unit = { _, _ -> },
    onDismiss: () -> Unit,
    onFoodLogged: (FoodLogEntry, CuratedFoodItem) -> Unit,
) {
    if (LocalConfiguration.current.screenWidthDp >= AroDialogWideMinScreenWidthDp) {
        // Wide windows (tablets/foldables): a centered, width-capped dialog is
        // easier to operate than a stretched bottom sheet.
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            AroDialogSurface {
                AddFoodDialogBody(
                    slot = slot,
                    foodLibrary = foodLibrary,
                    openFoodFactsClient = openFoodFactsClient,
                    targetDateLabel = targetDateLabel,
                    targetDateIsToday = targetDateIsToday,
                    planningMode = planningMode,
                    onRecordUsage = onRecordUsage,
                    onDismiss = onDismiss,
                    onFoodLogged = onFoodLogged,
                )
            }
        }
    } else {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            // Bounded height so the diary stays visible behind the sheet and
            // the sheet keeps a real drag-to-dismiss affordance.
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 720.dp),
        ) {
            AddFoodDialogBody(
                slot = slot,
                foodLibrary = foodLibrary,
                openFoodFactsClient = openFoodFactsClient,
                targetDateLabel = targetDateLabel,
                targetDateIsToday = targetDateIsToday,
                planningMode = planningMode,
                onRecordUsage = onRecordUsage,
                onDismiss = onDismiss,
                onFoodLogged = onFoodLogged,
            )
        }
    }
}

@Composable
private fun AddFoodDialogBody(
    slot: MealSlot,
    foodLibrary: OfflineFoodLibrary,
    openFoodFactsClient: OpenFoodFactsClient? = null,
    targetDateLabel: String = "",
    targetDateIsToday: Boolean = true,
    planningMode: Boolean = false,
    onRecordUsage: (CuratedFoodItem, Double) -> Unit = { _, _ -> },
    onDismiss: () -> Unit,
    onFoodLogged: (FoodLogEntry, CuratedFoodItem) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var isOnlineMode by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("All") }
    var categories by remember { mutableStateOf(listOf("All")) }
    var offlineResults by remember { mutableStateOf<List<CuratedFoodItem>>(emptyList()) }
    var onlineResults by remember { mutableStateOf<List<CuratedFoodItem>>(emptyList()) }
    var isOnlineLoading by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var configuringFood by remember { mutableStateOf<CuratedFoodItem?>(null) }
    var portionGramsText by remember { mutableStateOf("100") }
    var showScanner by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val cats = foodLibrary.getCategories()
        categories = listOf("All") + cats
    }

    LaunchedEffect(query, selectedCategory) {
        val catFilter = if (selectedCategory == "All") null else selectedCategory
        offlineResults = foodLibrary.search(query = query, category = catFilter)
    }

    LaunchedEffect(query, isOnlineMode) {
        if (isOnlineMode) {
            val cleanQ = query.trim()
            if (cleanQ.length >= 2 && openFoodFactsClient != null) {
                isOnlineLoading = true
                searchError = null
                delay(400)
                try {
                    onlineResults = openFoodFactsClient.searchProducts(cleanQ)
                    if (onlineResults.isEmpty()) {
                        searchError = "No products found on Open Food Facts for \"$cleanQ\""
                    }
                } catch (e: Exception) {
                    searchError = "Failed to query Open Food Facts: ${e.message}"
                } finally {
                    isOnlineLoading = false
                }
            } else {
                onlineResults = emptyList()
                searchError = null
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
                    coroutineScope.launch {
                        searchError = null
                        val offlineItem = foodLibrary.findByBarcode(barcode)
                        if (offlineItem != null) {
                            configuringFood = offlineItem
                            portionGramsText = (offlineItem.commonServing?.grams ?: 100.0).roundToInt().toString()
                            showScanner = false
                            return@launch
                        }
                        if (openFoodFactsClient != null) {
                            val onlineItem = openFoodFactsClient.getProductByBarcode(barcode)
                            if (onlineItem != null) {
                                configuringFood = onlineItem
                                portionGramsText = (onlineItem.commonServing?.grams ?: 100.0).roundToInt().toString()
                                showScanner = false
                                return@launch
                            }
                        }
                        searchError = "No product found for barcode: $barcode"
                        showScanner = false
                    }
                },
                onClose = { showScanner = false },
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val foodToConfig = configuringFood
            if (foodToConfig != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { configuringFood = null }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to list")
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Set Portion",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                }
            } else {
                Column {
                    Text(
                        text = "Add to ${slot.label}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = if (isOnlineMode) "Open Food Facts Live Catalog" else "Curated Whole Foods & Staples (Offline)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close dialog")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        if (targetDateLabel.isNotBlank()) {
            AroLoggingDateLabel(
                formattedDate = targetDateLabel,
                isToday = targetDateIsToday,
                labelPrefix = if (planningMode) "Planning for" else "Logging for",
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        val selectedFood = configuringFood
        if (selectedFood != null) {
            val currentGrams = portionGramsText.toDoubleOrNull()?.coerceAtLeast(1.0) ?: 100.0
            val entryPreview = selectedFood.toFoodLogEntry(meal = slot, grams = currentGrams)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = selectedFood.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${selectedFood.brand ?: "Whole Food"} • ${selectedFood.category ?: "Food"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )

                        if (selectedFood.nutriscore != null || selectedFood.nova != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                selectedFood.nutriscore?.let { NutriScoreBadge(it) }
                                selectedFood.nova?.let { NovaBadge(it) }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    selectedFood.commonServing?.let { common ->
                        OutlinedButton(
                            onClick = { portionGramsText = common.grams.roundToInt().toString() },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = "${common.name} (${common.grams.roundToInt()}g)",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { portionGramsText = "100" },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(0.7f),
                    ) {
                        Text("100g", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = { portionGramsText = "200" },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(0.7f),
                    ) {
                        Text("200g", style = MaterialTheme.typography.labelSmall)
                    }
                }

                OutlinedTextField(
                    value = portionGramsText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() } && input.length <= 5) {
                            portionGramsText = input
                        }
                    },
                    label = { Text("Grams (g)") },
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
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "NUTRITION FOR ${currentGrams.roundToInt()}g",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                        ) {
                            MacroMiniStat(
                                label = "Calories",
                                value = "${entryPreview.calories.roundToInt()} kcal",
                                color = AroTheme.macroColors.calories,
                            )
                            MacroMiniStat(
                                label = "Protein",
                                value = "${entryPreview.proteinG.roundToInt()}g",
                                color = AroTheme.macroColors.protein,
                            )
                            MacroMiniStat(
                                label = "Carbs",
                                value = "${entryPreview.nutrients.carbsG.roundToInt()}g",
                                color = AroTheme.macroColors.carbs,
                            )
                            MacroMiniStat(
                                label = "Fat",
                                value = "${entryPreview.nutrients.fatG.roundToInt()}g",
                                color = AroTheme.macroColors.fat,
                            )
                        }

                        if (entryPreview.nutrients.fiberG > 0 || entryPreview.nutrients.sodiumMg > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround,
                            ) {
                                MacroMiniStat(
                                    label = "Fiber",
                                    value = "${entryPreview.nutrients.fiberG.roundToInt()}g",
                                    color = AroTheme.macroColors.fiber,
                                )
                                MacroMiniStat(
                                    label = "Sodium",
                                    value = "${entryPreview.nutrients.sodiumMg.roundToInt()}mg",
                                    color = AroTheme.macroColors.clinicalModerate,
                                )
                                MacroMiniStat(
                                    label = "Potassium",
                                    value = "${entryPreview.nutrients.potassiumMg.roundToInt()}mg",
                                    color = AroTheme.macroColors.clinicalOptimal,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        if (!planningMode) onRecordUsage(selectedFood, currentGrams)
                        onFoodLogged(entryPreview, selectedFood)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (planningMode) {
                            "Plan to ${slot.label}"
                        } else {
                            "Log to ${slot.label}"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }
        } else {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = {
                    Text(
                        if (isOnlineMode) "Search Open Food Facts (e.g. Cheerios)..."
                        else "Search 40+ curated foods..."
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                        IconButton(onClick = { showScanner = true }) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan Barcode",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = !isOnlineMode,
                    onClick = {
                        isOnlineMode = false
                        searchError = null
                    },
                    label = { Text("Curated Whole Foods", style = MaterialTheme.typography.labelSmall) },
                    shape = RoundedCornerShape(8.dp),
                )
                FilterChip(
                    selected = isOnlineMode,
                    onClick = {
                        isOnlineMode = true
                        searchError = null
                    },
                    label = { Text("Open Food Facts (Live)", style = MaterialTheme.typography.labelSmall) },
                    shape = RoundedCornerShape(8.dp),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (!isOnlineMode) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(categories.size, key = { categories[it] }) { index ->
                        val cat = categories[index]
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(8.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (offlineResults.isEmpty() && query.isNotBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "No curated foods matching \"$query\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { isOnlineMode = true },
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Search Open Food Facts Online")
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(offlineResults.size, key = { offlineResults[it].id }) { index ->
                            val item = offlineResults[index]
                            FoodItemCard(
                                item = item,
                                slot = slot,
                                onSelect = {
                                    portionGramsText = (item.commonServing?.grams ?: 100.0).roundToInt().toString()
                                    configuringFood = item
                                },
                                onQuickAdd = {
                                    val grams = item.commonServing?.grams ?: item.servingSize
                                    onRecordUsage(item, grams)
                                    onFoodLogged(item.toFoodLogEntry(meal = slot, grams = grams), item)
                                },
                            )
                        }
                    }
                }
            } else {
                if (isOnlineLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Searching Open Food Facts...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else if (searchError != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = searchError!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else if (query.trim().length < 2) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Type at least 2 characters or scan a barcode to search Open Food Facts live database.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(onlineResults.size, key = { onlineResults[it].id }) { index ->
                            val item = onlineResults[index]
                            FoodItemCard(
                                item = item,
                                slot = slot,
                                onSelect = {
                                    portionGramsText = (item.commonServing?.grams ?: 100.0).roundToInt().toString()
                                    configuringFood = item
                                },
                                onQuickAdd = {
                                    val grams = item.commonServing?.grams ?: item.servingSize
                                    onRecordUsage(item, grams)
                                    onFoodLogged(item.toFoodLogEntry(meal = slot, grams = grams), item)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
